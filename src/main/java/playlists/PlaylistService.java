package playlists;

import database.DatabaseService;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;

import java.util.List;

@ProxyGen
@VertxGen
public interface PlaylistService {
    @GenIgnore
    static PlaylistService create(final DatabaseService databaseService, final FileSystem fileSystem) {
        return new PlaylistServiceImpl(databaseService, fileSystem);
    }

    @GenIgnore
    static PlaylistService createProxy(Vertx vertx, String address) {
        return new PlaylistServiceVertxEBProxy(vertx, address);
    }

    Future<List<JsonObject>> listPlaylists();

    Future<List<JsonObject>> playlistSongs(String playlistName);

    Future<Boolean> createPlaylist(String playlistName);

    Future<Boolean> deletePlaylist(String playlistName);

    Future<Boolean> renamePlaylist(String playlistName, String newName);

    Future<Boolean> addSongToPlaylist(String playlistName, String songPath);

    Future<Boolean> deleteSongFromPlaylist(String playlistName, String songPath);
}
