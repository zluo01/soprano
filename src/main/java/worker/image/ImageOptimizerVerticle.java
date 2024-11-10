package worker.image;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import worker.eventBus.ArtworkPayload;

import java.util.List;

import static config.ServerConfig.coverSourceDimension;
import static config.ServerConfig.coverVariants;
import static enums.WorkerAction.OPTIMIZE_IMAGE;
import static worker.image.ImageOptimizer.optimize;

public final class ImageOptimizerVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImageOptimizerVerticle.class);

    @Override
    public void start() {
        vertx.eventBus().<ArtworkPayload>consumer(OPTIMIZE_IMAGE.name(), message -> buildDirectory(message.body(), vertx.fileSystem()));
    }

    private void buildDirectory(final ArtworkPayload payload, final FileSystem fileSystem) {
        final int sourceDimension = coverSourceDimension(config());
        final List<Integer> variantDimensions = coverVariants(config());

        final String filePath = payload.destination();
        fileSystem.writeFile(filePath, Buffer.buffer(payload.imageData()))
                  .compose(__ -> vertx.executeBlocking(() -> optimize(filePath, sourceDimension, variantDimensions)))
                  .compose(status -> {
                      if (status) {
                          return fileSystem.delete(filePath);
                      }
                      return Future.failedFuture("Error happen when try to create thumbnail and cover from " + fileSystem);
                  })
                  .onSuccess(__ -> LOGGER.info("Successfully optimized image {}", filePath))
                  .onFailure(throwable -> LOGGER.error("Fail to optimize image", throwable));
    }
}

