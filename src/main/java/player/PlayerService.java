package player;

import database.DatabaseService;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

@ProxyGen
@VertxGen
public interface PlayerService {
    @GenIgnore
    static PlayerService create(final DatabaseService databaseService, final MPVInstance instance) {
        return new PlayerServiceImpl(databaseService, instance);
    }

    @GenIgnore
    static PlayerService createProxy(Vertx vertx, String address) {
        return new PlayerServiceVertxEBProxy(vertx, address);
    }

    Future<Integer> playSong(String songPath);

    Future<Integer> playPlaylist(String playlistName);

    Future<Integer> playAlbum(int albumId);

    Future<Integer> pauseSong();

    Future<Integer> nextSong();

    Future<Integer> prevSong();

    Future<Integer> playSongInQueueAtPosition(int position);

    Future<Integer> addSongToQueue(String songPath);

    Future<Integer> removeSongFromQueue(int position);

    Future<Integer> clearQueue();

    Future<List<JsonObject>> songsInQueue();

    Future<JsonObject> playbackStatus();
}
