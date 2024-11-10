package playlists;

import database.DatabaseService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FilenameUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static config.ServerConfig.PLAYLIST_PATH;

public class PlaylistServiceImpl implements PlaylistService {
    private static final String M3U_MATCH_REGEX = "^(?!\\.).*\\.m3u$";
    private static final Path BASE_PATH = Path.of(PLAYLIST_PATH);

    private final DatabaseService databaseService;
    private final FileSystem fileSystem;

    public PlaylistServiceImpl(final DatabaseService databaseService,
                               final FileSystem fileSystem) {
        this.databaseService = databaseService;
        this.fileSystem = fileSystem;
    }

    @Override
    public Future<List<JsonObject>> listPlaylists() {
        return fileSystem.readDir(PLAYLIST_PATH, M3U_MATCH_REGEX)
                         .compose(paths -> {
                             final var futures =
                                     paths.stream()
                                          .map(o -> fileSystem.props(o)
                                                              .map(props -> JsonObject.of("name", FilenameUtils.getBaseName(o),
                                                                                          "modifiedTime", props.lastModifiedTime())))
                                          .toList();

                             return Future.all(futures).map(CompositeFuture::<JsonObject>list);
                         });
    }

    @Override
    public Future<List<JsonObject>> playlistSongs(final String playlistName) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.readFile(filePath)
                         .flatMap(content -> {
                             final Map<String, Integer> orderMap = new HashMap<>();
                             final String[] songPaths = content.toString(StandardCharsets.UTF_8)
                                                               .trim()
                                                               .split("\n");
                             for (int i = 0; i < songPaths.length; i++) {
                                 final int idx = i;
                                 orderMap.computeIfAbsent(songPaths[i], s -> idx);
                             }

                             return databaseService.songsFromPath(new ArrayList<>(orderMap.keySet()))
                                                   .map(songs -> {
                                                       // reorder result to match playlist file order
                                                       final JsonObject[] songData = new JsonObject[songPaths.length];
                                                       for (JsonObject song : songs) {
                                                           final String path = song.getString("path");
                                                           songData[orderMap.get(path)] = song;
                                                       }
                                                       return Arrays.stream(songData)
                                                                    .filter(Objects::nonNull)
                                                                    .toList();
                                                   });
                         });
    }

    @Override
    public Future<Boolean> createPlaylist(final String playlistName) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.createFile(filePath).map(__ -> true);
    }

    @Override
    public Future<Boolean> deletePlaylist(final String playlistName) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.delete(filePath).map(__ -> true);
    }

    @Override
    public Future<Boolean> renamePlaylist(final String playlistName, final String newName) {
        if (playlistName.equals(newName)) {
            return Future.succeededFuture(true);
        }
        final String oldFilePath = resolvePlaylistFilePath(playlistName);
        final String newFilePath = resolvePlaylistFilePath(newName);
        return fileSystem.move(oldFilePath, newFilePath).map(__ -> true);
    }

    @Override
    public Future<Boolean> addSongToPlaylist(final String playlistName, final String songPath) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.readFile(filePath)
                         .map(content -> {
                             if (content.toString(StandardCharsets.UTF_8).contains(songPath)) {
                                 return Future.succeededFuture();
                             }
                             final Buffer newContent = content.appendString(songPath).appendString("\n");
                             return fileSystem.writeFile(filePath, newContent);
                         })
                         .map(__ -> true);
    }

    @Override
    public Future<Boolean> deleteSongFromPlaylist(final String playlistName, final String songPath) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.readFile(filePath)
                         .map(content -> {
                             final String[] songs = content.toString(StandardCharsets.UTF_8)
                                                           .trim()
                                                           .split("\n");
                             final StringBuilder sb = new StringBuilder();
                             for (final String song : songs) {
                                 if (song.equals(songPath)) {
                                     continue;
                                 }
                                 sb.append(song).append("\n");
                             }
                             return sb.toString();
                         })
                         .map(content -> fileSystem.writeFile(filePath, Buffer.buffer(content)))
                         .map(__ -> true);
    }

    private static String resolvePlaylistFilePath(final String fileName) {
        return BASE_PATH.resolve(fileName + ".m3u").toString();
    }
}
