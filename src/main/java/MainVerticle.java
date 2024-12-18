import collector.AudioDataCollectorVerticle;
import config.ServerConfig;
import database.DatabaseVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import player.PlayerVerticle;
import playlists.PlaylistVerticle;
import server.WebServerVerticle;

import java.util.logging.Level;

import static config.ServerConfig.verifyAndSetupConfig;

public final class MainVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(MainVerticle.class);

    static {
        // disable the annoying jaudiotagger logs
        java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    public static void main(final String[] args) {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));

        vertx.exceptionHandler(throwable -> LOGGER.fatal("Unhandled exception", throwable));
        vertx.deployVerticle(new MainVerticle())
             .onSuccess(deploymentId -> LOGGER.info("Deployment id for {} is: {}", MainVerticle.class, deploymentId))
             .onFailure(error -> {
                 LOGGER.fatal(error);
                 vertx.close();
             });
    }

    @Override
    public void start(final Promise<Void> promise) {
        final var configRetriever = configRetriever();
        validateSetup().compose(__ -> configRetriever.getConfig())
                       .compose(ServerConfig::verifyAndSetupConfig)
                       .compose(config -> deployEventLoopVertical(DatabaseVerticle.class, config)
                               .compose(__ -> Future.all(deployEventLoopVertical(WebServerVerticle.class, config),
                                                         deployEventLoopVertical(PlaylistVerticle.class, config),
                                                         deployVerticle(PlayerVerticle.class,
                                                                        new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
                                                                                               .setConfig(config)),
                                                         deployVerticle(AudioDataCollectorVerticle.class,
                                                                        new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
                                                                                               .setConfig(config)))))
                       .onSuccess(compositeFuture -> promise.complete())
                       .onFailure(promise::fail);

        configRetriever.listen(change -> verifyAndSetupConfig(change.getNewConfiguration())
                .onSuccess(newConfig -> config().mergeIn(newConfig))
                .onFailure(LOGGER::error));
    }

    private Future<Void> deployEventLoopVertical(final Class<?> verticleClass, final JsonObject config) {
        return deployVerticle(verticleClass, new DeploymentOptions().setConfig(config));
    }

    private Future<Void> deployVerticle(final Class<?> verticleClass,
                                        final DeploymentOptions deploymentOptions) {
        return vertx.deployVerticle(verticleClass.getName(), deploymentOptions)
                    .flatMap(__ -> Future.succeededFuture());
    }

    private Future<Void> validateSetup() {
        final FileSystem fileSystem = vertx.fileSystem();
        return Future.all(
                fileSystem.exists(ServerConfig.CONFIG_FILE_PATH).compose(exist -> {
                    if (exist) {
                        return Future.succeededFuture();
                    }
                    return Future.failedFuture("Cannot find config file: " + ServerConfig.CONFIG_FILE_PATH);
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
