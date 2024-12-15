package helper;

import java.nio.file.Path;

import static config.ServerConfig.PLAYLIST_PATH;

public final class PathHelper {
    private static final Path BASE_PATH = Path.of(PLAYLIST_PATH);

    private PathHelper() {
    }

    public static String resolvePlaylistFilePath(final String fileName) {
        return BASE_PATH.resolve(fileName + ".m3u").toString();
    }
}
