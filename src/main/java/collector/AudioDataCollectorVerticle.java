package collector;

import config.ServerConfig;
import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static collector.AlbumScannerHelper.parseTag;
import static collector.AlbumScannerHelper.retrieveSongPaths;
import static collector.ImageOptimizer.optimize;
import static config.ServerConfig.coverSourceDimension;
import static config.ServerConfig.coverVariants;
import static config.ServerConfig.musicDirectory;
import static enums.WorkerAction.BUILD_DIRECTORY;
import static enums.WorkerAction.SCAN_DIRECTORY;

public final class AudioDataCollectorVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(AudioDataCollectorVerticle.class);

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ExecutorService imageOptimizationExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private DatabaseService databaseService;
    private EventBus eventBus;
    private FileSystem fileSystem;

    @Override
    public void start() {
        databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        fileSystem = vertx.fileSystem();

        eventBus = vertx.eventBus();

        eventBus.consumer(SCAN_DIRECTORY.name(), message -> scanDirectory(musicDirectory(config())));

        eventBus.<String>consumer(BUILD_DIRECTORY.name(), message -> buildDatabase(message.body()));
    }

    @Override
    public void stop() {
        imageOptimizationExecutor.shutdown();
        try {
            if (!imageOptimizationExecutor.awaitTermination(30, TimeUnit.MINUTES)) {
                imageOptimizationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            imageOptimizationExecutor.shutdownNow();
        }
    }

    private void scanDirectory(final String root) {
        try {
            if (isRunning.get()) {
                LOGGER.info("Already scanning the directory.");
                return;
            }
            LOGGER.info("Start scanning directory: {}", root);
            isRunning.set(true);
            final List<String> songPaths = retrieveSongPaths(root);
            LOGGER.info("Start parsing {} song files.", songPaths.size());
            parseSongData(songPaths);
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve song paths from directory: {}", root, e);
        }
    }

    private void parseSongData(final List<String> songPaths) {
        new Thread(() -> {
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

            final String albumData = Json.encode(sourceMap.values()
                                                          .stream()
                                                          .map(AlbumData::toJson)
                                                          .toList());
            eventBus.publish(BUILD_DIRECTORY.name(), albumData);

            // optimize image
            visitedAlbum.forEach((albumId, artwork) -> imageOptimizationExecutor.execute(() -> optimizeImage(albumId, artwork)));
        }).start();
    }

    private void buildDatabase(final String payload) {
        final List<AlbumData> albumData = ((JsonArray) Json.decodeValue(payload))
                .stream()
                .map(o -> new AlbumData((JsonObject) o))
                .toList();
        databaseService.scan(albumData)
                       .onSuccess(__ -> LOGGER.info("Successfully update the database"))
                       .onFailure(throwable -> LOGGER.error("Fail to build directory", throwable))
                       .eventually(() -> {
                           isRunning.set(false);
                           return Future.succeededFuture();
                       });
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
