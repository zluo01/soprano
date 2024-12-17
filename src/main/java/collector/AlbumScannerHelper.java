package collector;

import models.SongData;
import org.apache.commons.io.FilenameUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AlbumScannerHelper {
    private static final Set<String> ALLOWED_EXT = Arrays.stream(SupportedFileFormat.values())
                                                         .map(SupportedFileFormat::getFilesuffix)
                                                         .collect(Collectors.toSet());

    private AlbumScannerHelper() {
    }

    static List<String> retrieveSongPaths(final String directoryPath) throws IOException {
        try (Stream<Path> walk = Files.walk(Path.of(directoryPath))) {
            return walk.map(Path::toFile)
                       .filter(File::isFile)
                       .filter(file -> !file.isHidden() && ALLOWED_EXT.contains(FilenameUtils.getExtension(file.getName())))
                       .map(File::getAbsolutePath)
                       .toList();
        }
    }

    static SongPayload parseTag(final String path) throws CannotReadException,
                                                          TagException,
                                                          InvalidAudioFrameException,
                                                          ReadOnlyFileException,
                                                          IOException {
        final Path filePath = Path.of(path);
        final AudioFile f = AudioFileIO.read(filePath.toFile());
        final Tag tag = f.getTag();

        final Artwork artwork = tag.getFirstArtwork();

        final BasicFileAttributes fileAttributes = Files.readAttributes(filePath, BasicFileAttributes.class);
        final int disc = parseFractionValue(tag.getFirst(FieldKey.DISC_NO));
        final int trackNum = parseFractionValue(tag.getFirst(FieldKey.TRACK));
        final var songData = SongData.builder()
                                     .path(path)
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
}
