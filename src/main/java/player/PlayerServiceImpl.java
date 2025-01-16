package player;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Future<Integer> toggleLoop(final int loopId) {
        int error;
        if (loopId == 0) {
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop", "no");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop-playlist", "no");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
        } else if (loopId == 1) {
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop", "no");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop-playlist", "inf");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
        } else {
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop", "inf");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
            error = mpv.instance().mpv_set_option_string(mpv.handle(), "loop-playlist", "no");
            if (error != 0) {
                return Future.failedFuture("Failed to set loop option: " + MPVError.getError(error));
            }
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
        final var playlistSongsProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "playlist");

        if (playlistSongsProperty == null) {
            return Future.failedFuture("Failed to get playlist songs.");
        }

        final JsonArray playlistSongs = new JsonArray(playlistSongsProperty.getString(0));

        final Map<String, JsonObject> pathMap = new HashMap<>();

        int currentPlayingIndex = -1;
        for (int i = 0; i < playlistSongs.size(); i++) {
            final JsonObject song = playlistSongs.getJsonObject(i);

            final String path = song.getString("filename");
            final boolean isCurrent = song.getBoolean("current", false);
            final boolean isPlaying = song.getBoolean("playing", false);
            pathMap.put(path, JsonObject.of(
                    "position", i,
                    "playing", isPlaying
            ));

            if (isCurrent) {
                currentPlayingIndex = i;
            }
        }

        if (currentPlayingIndex == -1) {
            return Future.failedFuture("Failed to get current playing index from playlists: " + playlistSongs.encode());
        }

        final int cIndex = currentPlayingIndex;
        return databaseService.songsFromPath(new ArrayList<>(pathMap.keySet()))
                              .flatMap(songs -> {
                                  if (songs.size() != pathMap.size()) {
                                      return Future.failedFuture("Mismatch on response song size. Expected " + pathMap.size() + " but got " + songs.size());
                                  }

                                  for (JsonObject song : songs) {
                                      final String path = song.getString("path");

                                      pathMap.computeIfPresent(path, (p, base) -> base.mergeIn(song));
                                  }

                                  final var response =
                                          pathMap.values()
                                                 .stream()
                                                 .sorted(Comparator.comparingInt(o -> o.getInteger("position")))
                                                 .collect(Collectors.toList());

                                  Collections.rotate(response, songs.size() - cIndex);
                                  return Future.succeededFuture(response);
                              });
    }

    @Override
    public Future<JsonObject> playbackStatus() {
        final var currentPlayingSongPathProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "path");

        final int loopId = loopId();
        if (loopId == -1) {
            return Future.failedFuture("Fail to get loop id");
        }

        if (currentPlayingSongPathProperty == null) {
            return Future.succeededFuture(JsonObject.of(
                    "playing", false,
                    "elapsed", 0,
                    "loopId", loopId
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
                                      "loopId", loopId,
                                      "song", song
                              ));
    }

    private int loopId() {
        final var loopStatusProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "loop");
        if (loopStatusProperty == null) {
            return -1;
        }
        final String loopStatus = loopStatusProperty.getString(0);

        final var loopPlaylistStatusProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "loop-playlist");
        if (loopPlaylistStatusProperty == null) {
            return -1;
        }
        final String loopPlaylistStatus = loopPlaylistStatusProperty.getString(0);

        if (loopStatus.equals("no") && loopPlaylistStatus.equals("no")) {
            return 0;
        }

        if (loopPlaylistStatus.equals("inf")) {
            return 1;
        }

        return 2;
    }
}
