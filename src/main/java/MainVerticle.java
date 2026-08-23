import collector.AudioDataCollectorVerticle;
import config.ServerConfig;
import database.DatabaseService;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import player.PlayerService;
import player.base.AudioPlayerFactory;
import playlists.PlaylistService;
import server.WebServerVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static config.ServerConfig.verifyAndSetupConfig;

public final class MainVerticle extends VerticleBase {

    static {
        // wire java logger to log4j
        // need to use JUL handler instead of java.util.logging.manager property
        // for runtime adapting for native support.
        Log4jBridgeHandler.install(true, null, true);
    }

    private static final Logger LOGGER = LogManager.getLogger(MainVerticle.class);

    private DatabaseService databaseService;
    private PlayerService playerService;

    static void main(final String[] args) {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
        vertx.exceptionHandler(throwable -> LOGGER.fatal("Unhandled exception", throwable));
        vertx.deployVerticle(new MainVerticle())
             .onFailure(error -> {
                 LOGGER.fatal("Startup Error", error);
                 vertx.close();
             });
    }

    @Override
    public Future<?> start() {
        final var configRetriever = configRetriever();
        final var startFuture = validateSetup()
                .compose(__ -> configRetriever.getConfig())
                .compose(ServerConfig::verifyAndSetupConfig)
                .compose(config -> {
                    databaseService = DatabaseService.create(vertx, config);
                    final PlaylistService playlistService = PlaylistService.create(vertx, databaseService);
                    playerService = PlayerService.create(databaseService, playlistService, AudioPlayerFactory.create(vertx, config));
                    return databaseService.initialization()
                                          .compose(__ -> playlistService.validatePlaylists())
                                          .compose(__ -> Future.all(deployEventLoopVertical(new WebServerVerticle(databaseService, playlistService, playerService), config),
                                                                    deployVerticle(new AudioDataCollectorVerticle(databaseService),
                                                                                   new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
                                                                                                          .setWorkerPoolName("Collector")
                                                                                                          .setMaxWorkerExecuteTime(1)
                                                                                                          .setMaxWorkerExecuteTimeUnit(TimeUnit.HOURS)
                                                                                                          .setWorkerPoolSize(1)
                                                                                                          .setConfig(config))));
                });

        configRetriever.listen(change -> verifyAndSetupConfig(change.getNewConfiguration())
                .onSuccess(newConfig -> config().mergeIn(newConfig))
                .onFailure(LOGGER::error));

        return startFuture;
    }

    @Override
    public Future<Void> stop() {
        final List<Future<?>> futures = new ArrayList<>();
        if (playerService != null) {
            futures.add(playerService.stop());
        }
        if (databaseService != null) {
            futures.add(databaseService.close());
        }
        return Future.all(futures).mapEmpty();
    }

    private Future<Void> deployEventLoopVertical(final Deployable deployable, final JsonObject config) {
        return deployVerticle(deployable, new DeploymentOptions().setConfig(config));
    }

    private Future<Void> deployVerticle(final Deployable deployable,
                                        final DeploymentOptions deploymentOptions) {
        return vertx.deployVerticle(deployable, deploymentOptions)
                    .flatMap(__ -> Future.succeededFuture());
    }

    private Future<Void> validateSetup() {
        final FileSystem fileSystem = vertx.fileSystem();
        return Future.all(
                fileSystem.exists(ServerConfig.BASE_CONFIG_PATH.toString()).compose(exist -> {
                    if (exist) {
                        return Future.succeededFuture();
                    }
                    return fileSystem.mkdir(ServerConfig.BASE_CONFIG_PATH.toString());
                }),
                fileSystem.exists(ServerConfig.COVER_PATH).compose(exist -> {
                    if (exist) {
                        return Future.succeededFuture();
                    }
                    return fileSystem.mkdir(ServerConfig.COVER_PATH);
                }),
                fileSystem.exists(ServerConfig.PLAYLIST_PATH).compose(exist -> {
                    if (exist) {
                        return Future.succeededFuture();
                    }
                    return fileSystem.mkdir(ServerConfig.PLAYLIST_PATH);
                }),
                fileSystem.exists(ServerConfig.CONFIG_FILE_PATH).compose(exist -> {
                    if (exist) {
                        return Future.succeededFuture();
                    }
                    return Future.failedFuture("Cannot find config file: " + ServerConfig.CONFIG_FILE_PATH);
                })).compose(__ -> Future.succeededFuture());
    }

    private ConfigRetriever configRetriever() {
        final ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setFormat("properties")
                .setType("file")
                .setConfig(new JsonObject().put("path", ServerConfig.CONFIG_FILE_PATH));

        final ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);

        return ConfigRetriever.create(vertx, options);
    }
}
