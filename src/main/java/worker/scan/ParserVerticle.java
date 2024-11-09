package worker.scan;

import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static config.ServerConfig.MUSIC_DIRECTORY_CONFIG;
import static enums.WorkerAction.OPTIMIZE_IMAGE;
import static enums.WorkerAction.SCAN_DIRECTORY;
import static worker.scan.AlbumScanner.constructSourceMap;

public final class ParserVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ParserVerticle.class);

    private DatabaseService databaseService;

    private EventBus eventBus;
    private FileSystem fileSystem;

    @Override
    public void start() {
        fileSystem = vertx.fileSystem();
        eventBus = vertx.eventBus();

        databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        vertx.eventBus().consumer(SCAN_DIRECTORY.name(), message -> scanDirectory(config().getString(MUSIC_DIRECTORY_CONFIG)));
    }

    private void scanDirectory(final String root) {
        vertx.executeBlocking(() -> constructSourceMap(root))
             .compose(response -> {
                 final List<Future<Void>> imageProcessFuture = response.artworks()
                                                                       .stream()
                                                                       .map(this::optimizeImage)
                                                                       .toList();

                 return databaseService.scan(response.albumData()).compose(__ -> Future.all(imageProcessFuture));
             })
             .onSuccess(__ -> LOGGER.info("Successfully update the database"))
             .onFailure(throwable -> LOGGER.error("Fail to scan directory", throwable));
    }

    private Future<Void> optimizeImage(final ArtworkPayload artworkPayload) {
        return fileSystem.writeFile(artworkPayload.destination(), Buffer.buffer(artworkPayload.imageData()))
                         .compose(__ -> {
                             eventBus.publish(OPTIMIZE_IMAGE.name(), artworkPayload.destination());
                             return Future.succeededFuture();
                         });
    }
}
