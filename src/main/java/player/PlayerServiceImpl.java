package player;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import models.Song;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import player.base.AudioPlayer;
import playlists.PlaylistService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PlayerServiceImpl implements PlayerService {
    private static final Logger LOGGER = LogManager.getLogger(PlayerServiceImpl.class.getName());

    private final DatabaseService databaseService;
    private final PlaylistService playlistService;
    private final AudioPlayer player;
    private final AtomicReference<PlayState> playState;

    public PlayerServiceImpl(final DatabaseService databaseService,
                             final PlaylistService playlistService,
                             final AudioPlayer player) {
        this.databaseService = databaseService;
        this.playlistService = playlistService;
        this.player = player;
        this.playState = new AtomicReference<>(PlayState.DEFAULT);


        this.player.startMonitor(() -> {
            final var stat = playState.updateAndGet(PlayState::next);
            return stat.currentSongPath();
        });
    }

    @Override
    public Future<Integer> playSong(final String songPath) {
        return databaseService.song(songPath)
                              .compose(song -> {
                                  final var stat = playState.updateAndGet(state -> state.newPlayList(List.of(song)));
                                  if (stat.currentSongPath().isEmpty()) {
                                      return Future.failedFuture("Failed to play song: " + songPath);
                                  }
                                  return player.play(stat.currentSongPath().get());
                              });
    }

    @Override
    public Future<Integer> playPlaylist(final String playlistName) {
        return playlistService.playlistSongs(playlistName)
                              .compose(songs -> {
                                  final var stat = playState.updateAndGet(state -> state.newPlayList(songs));
                                  if (stat.currentSongPath().isEmpty()) {
                                      return Future.failedFuture("Failed to play playlist: " + playlistName);
                                  }
                                  return player.play(stat.currentSongPath().get());
                              });
    }

    @Override
    public Future<Integer> playAlbum(final int albumId) {
        return databaseService.album(albumId)
                              .map(album -> album.songs().stream().map(Song::path).toList())
                              .flatMap(this::songsFromPath)
                              .compose(songs -> {
                                  final var stat = playState.updateAndGet(state -> state.newPlayList(Arrays.asList(songs)));
                                  if (stat.currentSongPath().isEmpty()) {
                                      return Future.failedFuture("Failed to play albumId: " + albumId);
                                  }
                                  return player.play(stat.currentSongPath().get());
                              });
    }

    @Override
    public Future<Integer> pauseSong() {
        return player.pause();
    }

    @Override
    public Future<Integer> nextSong() {
        final var stat = playState.updateAndGet(PlayState::next);
        if (stat.currentSongPath().isEmpty()) {
            LOGGER.warn("Next song is empty.");
            return Future.succeededFuture(0);
        }
        return player.play(stat.currentSongPath().get());
    }

    @Override
    public Future<Integer> prevSong() {
        final var stat = playState.updateAndGet(PlayState::previous);
        if (stat.currentSongPath().isEmpty()) {
            LOGGER.warn("Previous song is empty.");
            return Future.succeededFuture(0);
        }
        return player.play(stat.currentSongPath().get());
    }

    @Override
    public Future<Integer> cycleRepeatMode() {
        playState.updateAndGet(PlayState::cycleRepeatMode);
        return Future.succeededFuture(0);
    }

    @Override
    public Future<Integer> playSongInQueueAtPosition(final int position) {
        final var stat = playState.updateAndGet(state -> state.playAt(position));
        if (stat.currentSongPath().isEmpty()) {
            return Future.failedFuture("Failed to play song at position: " + position);
        }
        return player.play(stat.currentSongPath().get());
    }

    @Override
    public Future<Integer> addSongsToQueue(final List<String> songPaths) {
        return songsFromPath(songPaths)
                .compose(songs -> {
                    playState.updateAndGet(state -> state.addSongs(songs));
                    return Future.succeededFuture(0);
                });
    }

    @Override
    public Future<Integer> removeSongFromQueue(final int position) {
        playState.updateAndGet(state -> state.removeSong(position));
        return Future.succeededFuture(0);
    }

    @Override
    public Future<Integer> clearQueue() {
        playState.updateAndGet(PlayState::reset);
        return player.stop();
    }

    @Override
    public Future<List<JsonObject>> songsInQueue() {
        final var stat = playState.get();
        final var songs = IntStream.range(0, stat.playlist().size())
                                   .mapToObj(i -> {
                                       final var song = stat.playlist().get(i);
                                       return JsonObject.of(
                                               "position", i,
                                               "playing", stat.currentIndex() == i
                                       ).mergeIn(song);
                                   })
                                   .collect(Collectors.toList());
        Collections.rotate(songs, songs.size() - stat.currentIndex());
        return Future.succeededFuture(songs);
    }

    private Future<JsonObject[]> songsFromPath(final List<String> paths) {
        final JsonObject[] songObjects = new JsonObject[paths.size()];
        return databaseService.songsFromPath(paths)
                              .flatMap(songs -> {
                                  if (songs.size() != paths.size()) {
                                      return Future.failedFuture("Mismatch on response song size. Expected " + paths.size() + " but got " + songs.size());
                                  }
                                  for (JsonObject song : songs) {
                                      final String path = song.getString("path");

                                      final int idx = paths.indexOf(path);
                                      songObjects[idx] = song;
                                  }
                                  return Future.succeededFuture(songObjects);
                              });
    }

    @Override
    public Future<JsonObject> playbackStatus() {
        final var stat = playState.get();
        final var currentTrack = stat.currentSong();
        if (currentTrack.isEmpty()) {
            return Future.succeededFuture(JsonObject.of(
                    "playing", false,
                    "elapsed", 0,
                    "loopId", stat.repeatMode().ordinal()
            ));
        }
        return player.playbackStatus().map(playbackStatus -> JsonObject.of(
                "playing", playbackStatus.isPlaying(),
                "elapsed", playbackStatus.elapsed(),
                "loopId", stat.repeatMode().ordinal(),
                "song", currentTrack.get()
        ));
    }

    @Override
    public Future<Void> stop() {
        player.close();
        return Future.succeededFuture();
    }

    private record PlayState(List<JsonObject> playlist,
                             int currentIndex,
                             RepeatMode repeatMode) {

        PlayState {
            playlist = List.copyOf(playlist);
            if (playlist.isEmpty() || currentIndex < -1) {
                currentIndex = -1;
            }
            repeatMode = repeatMode != null ? repeatMode : RepeatMode.NONE;
        }

        static final PlayState DEFAULT = new PlayState(Collections.emptyList(), -1, RepeatMode.NONE);

        Optional<JsonObject> currentSong() {
            if (playlist.isEmpty() || invalidIndex(currentIndex)) {
                return Optional.empty();
            }
            return Optional.of(playlist.get(currentIndex));
        }

        Optional<String> currentSongPath() {
            return currentSong().map(o -> o.getString("path"));
        }

        PlayState next() {
            if (playlist.isEmpty()) {
                return this;
            }

            return switch (repeatMode) {
                case ONE -> this;
                case ALL -> {
                    int nextIndex = (currentIndex + 1) % playlist.size();
                    yield new PlayState(playlist, nextIndex, repeatMode);
                }
                case NONE -> {
                    if (currentIndex + 1 >= playlist.size()) {
                        yield new PlayState(playlist, -1, repeatMode);
                    }
                    yield new PlayState(playlist, currentIndex + 1, repeatMode);
                }
            };
        }

        PlayState previous() {
            if (playlist.isEmpty()) {
                return this;
            }

            return switch (repeatMode) {
                case ONE -> this;
                case ALL -> {
                    int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
                    yield new PlayState(playlist, prevIndex, repeatMode);
                }
                case NONE -> {
                    if (currentIndex <= 0) {
                        yield this;
                    }
                    yield new PlayState(playlist, currentIndex - 1, repeatMode);
                }
            };
        }

        PlayState playAt(final int index) {
            if (invalidIndex(index)) {
                return this;
            }
            return new PlayState(playlist, index, repeatMode);
        }

        PlayState newPlayList(final List<JsonObject> songs) {
            return new PlayState(songs, 0, repeatMode);
        }

        PlayState reset() {
            return new PlayState(List.of(), -1, repeatMode);
        }

        //        public PlayState shuffle() {
        //            if (isEmpty())
        //                return this;
        //
        //            JsonObject currentTrack = playlist.get(currentIndex);
        //            List<JsonObject> shuffled = new ArrayList<>(playlist);
        //            Collections.shuffle(shuffled);
        //
        //            int newIndex = shuffled.indexOf(currentTrack);
        //            return new PlaylistState(shuffled, newIndex >= 0 ? newIndex : 0);
        //        }

        PlayState addSongs(final JsonObject... songs) {
            final var newSongs = Arrays.stream(songs)
                                       .filter(song -> !playlist.contains(song))
                                       .toList();

            if (newSongs.isEmpty()) {
                return this;
            }

            List<JsonObject> updated = new ArrayList<>(playlist);
            updated.addAll(newSongs);
            return new PlayState(updated, currentIndex, repeatMode);
        }

        PlayState cycleRepeatMode() {
            return new PlayState(playlist, currentIndex, repeatMode.next());
        }

        PlayState removeSong(final int index) {
            if (invalidIndex(index)) {
                return this;
            }

            final List<JsonObject> updated = new ArrayList<>(playlist);
            updated.remove(index);

            int newIndex;
            if (updated.isEmpty()) {
                newIndex = 0;
            } else if (index < currentIndex) {
                newIndex = currentIndex - 1;
            } else if (index == currentIndex) {
                newIndex = Math.min(currentIndex, updated.size() - 1);
            } else {
                newIndex = currentIndex;
            }

            return new PlayState(updated, newIndex, repeatMode);
        }

        private boolean invalidIndex(final int index) {
            return index < 0 || index >= playlist.size();
        }
    }

    private enum RepeatMode {
        NONE, ONE, ALL;

        RepeatMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
