package worker.scan;

import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import models.AlbumData;
import models.SongData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.tag.images.Artwork;
import worker.eventBus.ArtworkPayload;
import worker.eventBus.ArtworkPayloadCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static config.ServerConfig.MUSIC_DIRECTORY_CONFIG;
import static enums.WorkerAction.OPTIMIZE_IMAGE;
import static enums.WorkerAction.SCAN_DIRECTORY;
import static worker.scan.AlbumScannerHelper.parseTag;
import static worker.scan.AlbumScannerHelper.retrieveSongPaths;

public final class ScannerVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ScannerVerticle.class);

    private DatabaseService databaseService;

    private EventBus eventBus;
    private FileSystem fileSystem;

    @Override
    public void start() {
        fileSystem = vertx.fileSystem();
        eventBus = vertx.eventBus();

        vertx.eventBus().registerDefaultCodec(ArtworkPayload.class, new ArtworkPayloadCodec());

        databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        vertx.eventBus().consumer(SCAN_DIRECTORY.name(), message -> scanDirectory(config().getString(MUSIC_DIRECTORY_CONFIG)));
    }

    private void scanDirectory(final String root) {


        retrieveSongPaths(fileSystem, root)
                .compose(paths -> {
                    LOGGER.info("Start parsing {} song files.", paths.size());
                    return parseSongData(paths).compose(albumData -> databaseService.scan(albumData));
                })
                .onSuccess(__ -> LOGGER.info("Successfully update the database"))
                .onFailure(throwable -> LOGGER.error("Fail to scan directory", throwable));
    }

    private Future<List<AlbumData>> parseSongData(final List<String> paths) {
        final Map<Integer, AlbumData> sourceMap = new ConcurrentHashMap<>();
        final Map<Integer, Artwork> visitedAlbum = new ConcurrentHashMap<>();
        return vertx.executeBlocking(() -> {
            for (final String path : paths) {
                try {
                    final AlbumScannerHelper.SongPayload songPayload = parseTag(path);

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

            // optimize image
            visitedAlbum.entrySet()
                        .stream()
                        .map(o -> ArtworkPayload.of(o.getKey(), o.getValue()))
                        .forEach(payload -> eventBus.publish(OPTIMIZE_IMAGE.name(), payload));

            return new ArrayList<>(sourceMap.values());
        });
    }
}
