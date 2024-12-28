package player;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static helper.PathHelper.resolvePlaylistFilePath;

/**
 * <a href="https://mpv.io/manual/master/#playlist-manipulation">playlist commands</a>
 * <a href="https://mpv.io/manual/master/#properties">MPV properties</a>
 */
public class PlayerServiceImpl implements PlayerService {
    private final DatabaseService databaseService;
    private final MPVInstance mpv;

    public PlayerServiceImpl(final DatabaseService databaseService,
                             final MPVInstance mpv) {
        this.databaseService = databaseService;
        this.mpv = mpv;
    }

    @Override
    public Future<Integer> playSong(final String songPath) {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"loadfile", songPath});
        if (error != 0) {
            return Future.failedFuture("Failed to load file: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> playPlaylist(final String playlistName) {
        final var playlistFilePath = resolvePlaylistFilePath(playlistName);
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"loadlist", playlistFilePath});
        if (error != 0) {
            return Future.failedFuture("Failed to load file: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> playAlbum(final int albumId) {
        return databaseService.album(albumId)
                              .compose(album -> {
                                  int error = MPVError.MPV_ERROR_SUCCESS.ordinal();
                                  final var songs = album.songs();
                                  for (int i = 0; i < songs.size(); i++) {
                                      final var songPath = songs.get(i).path();
                                      String[] commands;
                                      if (i == 0) {
                                          commands = new String[]{"loadfile", songPath};
                                      } else {
                                          commands = new String[]{"loadfile", songPath, "append"};
                                      }

                                      error = mpv.instance().mpv_command(mpv.handle(), commands);
                                      if (error != 0) {
                                          return Future.failedFuture("Failed to load file: " + MPVError.getError(error));
                                      }
                                  }
                                  return Future.succeededFuture(error);
                              });
    }

    @Override
    public Future<Integer> pauseSong() {
        final var pauseProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "pause");
        if (pauseProperty == null) {
            return Future.failedFuture("Failed to get pause status.");
        }
        final var pauseStatus = pauseProperty.getString(0).equals("yes") ? "no" : "yes";
        final int error = mpv.instance().mpv_set_property_string(mpv.handle(), "pause", pauseStatus);
        if (error != 0) {
            return Future.failedFuture("Failed to pause: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> nextSong() {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"playlist-next"});
        if (error != 0) {
            return Future.failedFuture("Failed to play next song: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> prevSong() {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"playlist-prev"});
        if (error != 0) {
            return Future.failedFuture("Failed to play prev song: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> playSongInQueueAtPosition(final int position) {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"playlist-play-index", position + ""});
        if (error != 0) {
            return Future.failedFuture("Failed to add song to queue: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> addSongsToQueue(final List<String> songPaths) {
        int error = MPVError.MPV_ERROR_SUCCESS.ordinal();
        for (String songPath : songPaths) {
            error = mpv.instance().mpv_command(mpv.handle(), new String[]{"loadfile", songPath, "append-play"});
            if (error != 0) {
                return Future.failedFuture("Failed to add song to queue: " + MPVError.getError(error));
            }
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> removeSongFromQueue(final int position) {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"playlist-remove", position + ""});
        if (error != 0) {
            return Future.failedFuture("Failed to remove song from queue: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<Integer> clearQueue() {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"stop"});
        if (error != 0) {
            return Future.failedFuture("Failed to clear queue: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<List<JsonObject>> songsInQueue() {
        final var playlistCountProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "playlist/count");

        if (playlistCountProperty == null) {
            return Future.failedFuture("Failed to find playlist count");
        }
        final Map<String, Integer> orderMap = new HashMap<>();

        final int size = Integer.parseInt(playlistCountProperty.getString(0));
        for (int i = 0; i < size; i++) {
            final int idx = i;
            final var playlistFileName = mpv.instance().mpv_get_property_string(mpv.handle(), "playlist/" + i + "/filename");
            if (playlistFileName != null) {
                orderMap.computeIfAbsent(playlistFileName.getString(0), s -> idx);
            }
        }

        final var playlistPosProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "playlist-pos");
        if (playlistPosProperty == null) {
            return Future.failedFuture("Failed to find playlist pos");
        }
        final int playlistPos = Integer.parseInt(playlistPosProperty.getString(0));

        return databaseService.songsFromPath(new ArrayList<>(orderMap.keySet()))
                              .map(songs -> {
                                  // reorder result to match queue order
                                  final JsonObject[] songData = new JsonObject[orderMap.size()];
                                  for (JsonObject song : songs) {
                                      final String path = song.getString("path");
                                      final int position = orderMap.get(path);
                                      final boolean isPlaying = playlistPos == position;

                                      final JsonObject additionalSongInfo = JsonObject.of(
                                              "position", position,
                                              "playing", isPlaying
                                      );

                                      songData[orderMap.get(path)] = song.mergeIn(additionalSongInfo);
                                  }
                                  final var response = Arrays.stream(songData)
                                                             .filter(Objects::nonNull)
                                                             .collect(Collectors.toList());

                                  Collections.rotate(response, songs.size() - playlistPos);
                                  return response;
                              });
    }

    @Override
    public Future<JsonObject> playbackStatus() {
        final var currentPlayingSongPathProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "path");
        if (currentPlayingSongPathProperty == null) {
            return Future.succeededFuture(JsonObject.of(
                    "playing", false,
                    "elapsed", 0
            ));
        }
        final var songPath = currentPlayingSongPathProperty.getString(0);

        final var pauseProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "pause");
        if (pauseProperty == null) {
            return Future.failedFuture("Failed to get pause status.");
        }
        final var isPlaying = pauseProperty.getString(0).equals("no");

        final var playbackTimeProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "playback-time");
        if (playbackTimeProperty == null) {
            return Future.failedFuture("Fail to get playback time");
        }
        final double playbackTime = Double.parseDouble(playbackTimeProperty.getString(0));

        return databaseService.song(songPath)
                              .map(song -> JsonObject.of(
                                      "playing", isPlaying,
                                      "elapsed", (int) playbackTime,
                                      "song", song
                              ));
    }
}
