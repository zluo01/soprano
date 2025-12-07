package collector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import config.ServerConfig;
import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import models.AlbumData;
import models.SongData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.images.Artwork;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static collector.AlbumScannerHelper.parseTag;
import static collector.AlbumScannerHelper.retrieveSongPaths;
import static collector.ImageOptimizer.optimize;
import static config.ServerConfig.coverSourceDimension;
import static config.ServerConfig.coverVariants;
import static config.ServerConfig.musicDirectory;
import static enums.WorkerAction.DATABASE_UPDATE;
import static enums.WorkerAction.SCAN_DIRECTORY;
import static enums.WorkerAction.UPDATE_DIRECTORY;

public final class AudioDataCollectorVerticle extends VerticleBase {
    private static final Logger LOGGER = LogManager.getLogger(AudioDataCollectorVerticle.class);

    private static final ExecutorService IMAGE_OPTIMIZATION_EXECUTOR =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                                         new ThreadFactoryBuilder()
                                                 .setNameFormat("image-optimization-%d")
                                                 .setDaemon(true)
                                                 .build());

    private volatile boolean running = false;

    private final DatabaseService databaseService;

    private FileSystem fileSystem;
    private EventBus eventBus;

    public AudioDataCollectorVerticle(final DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Future<?> start() {
        fileSystem = vertx.fileSystem();
        eventBus = vertx.eventBus();

        eventBus.consumer(UPDATE_DIRECTORY.name(), __ -> scanDirectory(musicDirectory(config()), true));

        eventBus.consumer(SCAN_DIRECTORY.name(), __ -> scanDirectory(musicDirectory(config()), false));

        return Future.succeededFuture();
    }

    @Override
    public Future<?> stop() throws Exception {
        IMAGE_OPTIMIZATION_EXECUTOR.shutdown();
        try {
            if (!IMAGE_OPTIMIZATION_EXECUTOR.awaitTermination(30, TimeUnit.MINUTES)) {
                IMAGE_OPTIMIZATION_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            IMAGE_OPTIMIZATION_EXECUTOR.shutdownNow();
        }
        return super.stop();
    }

    private void scanDirectory(final String root, final boolean update) {
        try {
            if (running) {
                LOGGER.info("Already scanning the directory.");
                return;
            }
            LOGGER.info("Start scanning directory: {}", root);
            running = true;
            final List<String> songPaths = retrieveSongPaths(root);

            if (update) {
                databaseService.songPaths()
                               .flatMap(songPathsFromDatabase -> {
                                   final List<String> songPathsNotInDB = songPaths.stream()
                                                                                  .filter(o -> !songPathsFromDatabase.contains(o))
                                                                                  .toList();

                                   final List<String> deletedSongPaths = songPathsFromDatabase.stream()
                                                                                              .filter(o -> !songPaths.contains(o))
                                                                                              .toList();
                                   if (!deletedSongPaths.isEmpty()) {
                                       LOGGER.info("{} song need to delete.", deletedSongPaths.size());
                                       return databaseService.removeSongs(deletedSongPaths)
                                                             .map(__ -> songPathsNotInDB);
                                   }
                                   return Future.succeededFuture(songPathsNotInDB);
                               })
                               .onSuccess(songPathsNotInDB -> {
                                   if (!songPathsNotInDB.isEmpty()) {
                                       LOGGER.info("{} songs to add.", songPathsNotInDB.size());
                                       parseSongData(songPathsNotInDB);
                                   } else {
                                       LOGGER.info("Nothing to update.");
                                       eventBus.publish(DATABASE_UPDATE.name(), true);
                                       running = false;
                                   }
                               })
                               .onFailure(LOGGER::error);
            } else {
                databaseService.clearDatabase()
                               .onSuccess(__ -> {
                                   LOGGER.info("Start parsing {} song files.", songPaths.size());
                                   parseSongData(songPaths);
                               })
                               .onFailure(LOGGER::error);

            }
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve song paths from directory: {}", root, e);
        }
    }

    private void parseSongData(final List<String> songPaths) {
        final Map<Integer, AlbumData> sourceMap = new HashMap<>();
        final Map<Integer, Artwork> visitedAlbum = new HashMap<>();

        for (final String path : songPaths) {
            try {
                LOGGER.info("Parsing song: {}", path);
                final SongPayload songPayload = parseTag(path);

                final SongData song = songPayload.song();
                final String albumArtist = song.albumArtist();
                final int key = Objects.hash(song.album(), song.albumArtist());

                sourceMap.compute(key, (k, albumData) -> {
                    if (albumData == null) {
                        albumData = AlbumData.builder()
                                             .name(song.album())
                                             .artist(albumArtist.isEmpty() ? song.artist() : albumArtist)
                                             .date(song.date())
                                             .totalDuration(song.duration())
                                             .songs(new ArrayList<>())
                                             .atime(song.atime())
                                             .mtime(song.mtime())
                                             .build();
                    }
                    albumData.addSong(song);
                    albumData.incrementTotalDuration(song.duration());
                    albumData.updateAddTime(song.atime());
                    albumData.updateModifiedTime(song.mtime());
                    return albumData;
                });

                // aggregate album artwork
                visitedAlbum.computeIfAbsent(key, k -> songPayload.artwork());
            } catch (Exception e) {
                LOGGER.error("Fail to parse song data", e);
            }
        }

        databaseService.scan(List.copyOf(sourceMap.values()))
                       .compose(__ -> {
                           LOGGER.info("Successfully update the database");
                           optimizeImages(visitedAlbum);
                           return Future.succeededFuture();
                       })
                       .onSuccess(__ -> {
                           LOGGER.info("Finish scanning the directory.");
                           eventBus.publish(DATABASE_UPDATE.name(), true);
                       })
                       .onFailure(throwable -> {
                           LOGGER.error("Fail to build directory", throwable);
                           eventBus.publish(DATABASE_UPDATE.name(), false);
                       })
                       .eventually(() -> {
                           running = false;
                           return Future.succeededFuture();
                       });
    }

    private void optimizeImages(final Map<Integer, Artwork> coverMap) {
        if (coverMap.isEmpty()) {
            LOGGER.info("All images have been optimized.");
            return;
        }

        final var futures = coverMap.entrySet()
                                    .stream()
                                    .map(entry -> CompletableFuture.runAsync(
                                            () -> optimizeImage(entry.getKey(), entry.getValue()),
                                            IMAGE_OPTIMIZATION_EXECUTOR))
                                    .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES);
            LOGGER.info("Image optimization completed.");
        } catch (TimeoutException e) {
            LOGGER.error("Image optimization timed out after 30 minutes", e);
            futures.forEach(f -> f.cancel(true));
        } catch (Exception e) {
            LOGGER.error("Error happens during optimizing images", e);
        }
    }

    private void optimizeImage(final int albumId, final Artwork artwork) {
        try {
            final int sourceDimension = coverSourceDimension(config());
            final List<Integer> variantDimensions = coverVariants(config());

            final String filePath = Path.of(ServerConfig.COVER_PATH)
                                        .resolve(albumId + ".png")
                                        .toString();

            fileSystem.writeFileBlocking(filePath, Buffer.buffer(artwork.getBinaryData()));
            if (optimize(filePath, sourceDimension, variantDimensions)) {
                fileSystem.deleteBlocking(filePath);
                LOGGER.info("Successfully optimized image {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Fail to optimize image", e);
        }
    }
}
