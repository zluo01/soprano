package playlists;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface PlaylistService {
    static PlaylistService create(final Vertx vertx, final DatabaseService databaseService) {
        final FileSystem fileSystem = vertx.fileSystem();
        return new PlaylistServiceImpl(databaseService, fileSystem);
    }

    Future<Void> validatePlaylists();

    Future<List<JsonObject>> listPlaylists();

    Future<List<JsonObject>> playlistSongs(String playlistName);

    Future<Boolean> createPlaylist(String playlistName);

    Future<Boolean> deletePlaylist(String playlistName);

    Future<Boolean> renamePlaylist(String playlistName, String newName);

    Future<Boolean> addSongToPlaylist(String playlistName, String songPath);

    Future<Boolean> deleteSongFromPlaylist(String playlistName, String songPath);
}
