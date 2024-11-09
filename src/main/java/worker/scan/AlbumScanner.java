package worker.scan;

import io.vertx.core.eventbus.EventBus;
import models.AlbumData;
import models.SongData;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class AlbumScanner {
    private static final Logger LOGGER = LogManager.getLogger(AlbumScanner.class);

    private static final Set<String> ALLOWED_EXT = Set.of("mp3", "wav", "flac", "ape", "m4a", "wma", "dsf");

    private AlbumScanner() {
    }

    static ScanResponse constructSourceMap(final String root, final EventBus eventBus) throws IOException {
        final Map<Integer, AlbumData> sourceMap = new ConcurrentHashMap<>();
        final Map<Integer, Artwork> visitedAlbum = new ConcurrentHashMap<>();
        LOGGER.info("Start scanning directory: {}", root);
        try (Stream<Path> walk = Files.walk(Path.of(root))) {
            walk.parallel()
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(file -> !file.isHidden())
                .filter(o -> ALLOWED_EXT.contains(FilenameUtils.getExtension(o.getName())))
                .map(File::toPath)
                .forEach(p -> {
                    try {
                        final SongPayload songPayload = parseTag(p);
                        final SongData song = songPayload.song;
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
                    } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                             | InvalidAudioFrameException e) {
                        LOGGER.fatal("Fail to read song information", e);
                    }
                });
        }

        LOGGER.info("Finish building source map.");
        return new ScanResponse(new ArrayList<>(sourceMap.values()),
                                visitedAlbum.entrySet()
                                            .stream()
                                            .map(o -> ArtworkPayload.of(o.getKey(), o.getValue()))
                                            .toList());
    }

    private static SongPayload parseTag(final Path path) throws CannotReadException,
                                                                TagException,
                                                                InvalidAudioFrameException,
                                                                ReadOnlyFileException,
                                                                IOException {
        final AudioFile f = AudioFileIO.read(path.toFile());
        final Tag tag = f.getTag();

        final Artwork artwork = tag.getFirstArtwork();

        final BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        final int disc = parseFractionValue(tag.getFirst(FieldKey.DISC_NO));
        final int trackNum = parseFractionValue(tag.getFirst(FieldKey.TRACK));
        final var songData = SongData.builder()
                                     .path(path.toString())
                                     .artist(tag.getFirst(FieldKey.ARTIST))
                                     .album(tag.getFirst(FieldKey.ALBUM))
                                     .albumArtist(tag.getFirst(FieldKey.ALBUM_ARTIST))
                                     .title(tag.getFirst(FieldKey.TITLE))
                                     .genre(tag.getFirst(FieldKey.GENRE))
                                     .date(tag.getFirst(FieldKey.YEAR))
                                     .composer(tag.getFirst(FieldKey.COMPOSER))
                                     .performer(tag.getFirst(FieldKey.PERFORMER))
                                     .disc(disc)
                                     .trackNum(trackNum)
                                     .duration(f.getAudioHeader().getTrackLength())
                                     .mtime(fileAttributes.lastModifiedTime().toMillis())
                                     .atime(fileAttributes.creationTime().toMillis())
                                     .build();

        return new SongPayload(songData, artwork);
    }

    private static int parseFractionValue(final String value) {
        if (value.isEmpty()) {
            return 1;
        }
        if (value.contains("/")) {
            return Integer.parseInt(value.split("/", -1)[0]);
        }
        return Integer.parseInt(value);
    }

    private record SongPayload(SongData song, Artwork artwork) {
    }
}
