package playlists;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static config.ServerConfig.PLAYLIST_PATH;
import static helper.PathHelper.resolvePlaylistFilePath;

public class PlaylistServiceImpl implements PlaylistService {
    private static final Logger LOGGER = LogManager.getLogger(PlaylistServiceImpl.class);

    private static final String M3U_MATCH_REGEX = "^(?!\\.).*\\.m3u$";

    private final DatabaseService databaseService;
    private final FileSystem fileSystem;

    public PlaylistServiceImpl(final DatabaseService databaseService,
                               final FileSystem fileSystem) {
        this.databaseService = databaseService;
        this.fileSystem = fileSystem;
    }

    @Override
    public Future<Void> validatePlaylists() {
        return fileSystem.readDir(PLAYLIST_PATH, M3U_MATCH_REGEX)
                         .compose(paths -> {
                             final var futures = paths.stream()
                                                      .map(this::verifyPlaylist)
                                                      .toList();
                             return Future.all(futures).compose(__ -> Future.succeededFuture());
                         });
    }

    private Future<Void> verifyPlaylist(final String path) {
        return fileSystem.readFile(path)
                         .compose(content -> {
                             final List<String> songPaths = Arrays.stream(content.toString(StandardCharsets.UTF_8)
                                                                                 .trim()
                                                                                 .split("\n"))
                                                                  .filter(s -> !Strings.isBlank(s.trim()))
                                                                  .toList();
                             if (!songPaths.isEmpty()) {
                                 return databaseService.songsFromPath(songPaths)
                                                       .flatMap(songs -> {
                                                           if (songs.size() != songPaths.size()) {
                                                               final var songsSet = new HashSet<>(songPaths);
                                                               songsSet.removeAll(songs.stream()
                                                                                       .map(o -> o.getString("path"))
                                                                                       .collect(Collectors.toSet()));
                                                               LOGGER.warn("Missing song in database {}", songsSet);
                                                               return fileSystem.move(path, path + ".bak");
                                                           }
                                                           return Future.succeededFuture();
                                                       });
                             }
                             return Future.succeededFuture();
                         });
    }

    @Override
    public Future<List<JsonObject>> listPlaylists() {
        return fileSystem.readDir(PLAYLIST_PATH, M3U_MATCH_REGEX)
                         .compose(paths -> {
                             final var futures =
                                     paths.stream()
                                          .map(this::playlistDetail)
                                          .toList();

                             return Future.all(futures)
                                          .map(compositeFuture -> compositeFuture.<JsonObject>list()
                                                                                 .stream()
                                                                                 .filter(Objects::nonNull)
                                                                                 .toList());
                         });
    }

    private Future<JsonObject> playlistDetail(final String path) {
        final var payload = JsonObject.of("name", FilenameUtils.getBaseName(path));
        final var futures = List.of(
                fileSystem.props(path).map(props -> JsonObject.of("modifiedTime", props.lastModifiedTime())),
                fileSystem.readFile(path)
                          .compose(content -> {
                              final List<String> songPaths = Arrays.stream(content.toString(StandardCharsets.UTF_8)
                                                                                  .trim()
                                                                                  .split("\n"))
                                                                   .filter(s -> !Strings.isBlank(s))
                                                                   .toList();
                              final int size = songPaths.size();
                              if (!songPaths.isEmpty()) {
                                  return databaseService.song(songPaths.getFirst())
                                                        .map(song -> {
                                                            if (song.isEmpty()) {
                                                                return song;
                                                            }
                                                            return JsonObject.of("coverId", song.getInteger("albumId"),
                                                                                 "songCount", size);
                                                        });
                              }
                              return Future.succeededFuture(JsonObject.of("songCount", size));
                          })
        );
        return Future.all(futures)
                     .map(compositeFuture -> {
                         for (JsonObject resp : compositeFuture.<JsonObject>list()) {
                             if (resp.isEmpty()) {
                                 return null;
                             }
                             payload.mergeIn(resp);
                         }
                         return payload;
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
                             final List<String> songs = Arrays.stream(content.toString(StandardCharsets.UTF_8)
                                                                             .trim()
                                                                             .split("\n"))
                                                              .collect(Collectors.toList());
                             if (songs.contains(songPath)) {
                                 return Future.succeededFuture();
                             }
                             songs.add(songPath);
                             final Buffer newContent = Buffer.buffer(String.join("\n", songs));
                             return fileSystem.writeFile(filePath, newContent);
                         })
                         .map(__ -> true);
    }

    @Override
    public Future<Boolean> deleteSongFromPlaylist(final String playlistName, final String songPath) {
        final String filePath = resolvePlaylistFilePath(playlistName);
        return fileSystem.readFile(filePath)
                         .map(content -> Arrays.stream(content.toString(StandardCharsets.UTF_8)
                                                              .trim()
                                                              .split("\n"))
                                               .filter(path -> !path.equals(songPath))
                                               .collect(Collectors.joining("\n")))
                         .map(content -> fileSystem.writeFile(filePath, Buffer.buffer(content)))
                         .map(__ -> true);
    }

}
