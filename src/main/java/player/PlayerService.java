package player;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.reactivestreams.Publisher;
import player.base.AudioPlayer;
import playlists.PlaylistService;

import java.util.List;

public interface PlayerService {
    static PlayerService create(final DatabaseService databaseService,
                                final PlaylistService playlistService,
                                final AudioPlayer player) {
        return new PlayerServiceImpl(databaseService, playlistService, player);
    }

    Future<Integer> playSong(String songPath);

    Future<Integer> playPlaylist(String playlistName);

    Future<Integer> playAlbum(int albumId);

    Future<Integer> pauseSong();

    Future<Integer> nextSong();

    Future<Integer> prevSong();

    Future<Integer> cycleRepeatMode();

    Future<Integer> playSongInQueueAtPosition(int position);

    Future<Integer> addSongsToQueue(List<String> songPaths);

    Future<Integer> removeSongFromQueue(int position);

    Future<Integer> clearQueue();

    Future<List<JsonObject>> songsInQueue();

    Future<JsonObject> playbackStatus();

    Publisher<Boolean> songUpdates();

    Future<Void> stop();
}
