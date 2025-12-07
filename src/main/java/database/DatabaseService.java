package database;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import models.Album;
import models.AlbumData;

import java.util.List;

import static config.ServerConfig.DATABASE_CONFIG;
import static config.ServerConfig.DATABASE_FILE_PATH;


public interface DatabaseService {
    static DatabaseService create(final Vertx vertx, final JsonObject config) {
        final String url = config.getString(DATABASE_CONFIG, DATABASE_FILE_PATH);

        final JDBCConnectOptions connectOptions = new JDBCConnectOptions()
                .setJdbcUrl("jdbc:sqlite:" + url);
        final Pool client = JDBCPool.pool(vertx, connectOptions, new PoolOptions());
        return new DatabaseServiceImpl(client);
    }

    Future<Void> initialization();

    Future<List<Album>> albums();

    Future<Album> album(int id);

    Future<List<JsonObject>> genres();

    Future<List<JsonObject>> albumsForGenre(int id);

    Future<List<JsonObject>> albumArtists();

    Future<List<JsonObject>> albumsForAlbumArtist(int id);

    Future<List<JsonObject>> artists();

    Future<List<JsonObject>> albumsForArtist(int id);

    Future<Void> scan(List<AlbumData> albums);

    Future<List<JsonObject>> songsFromPath(List<String> paths);

    Future<JsonObject> stats();

    Future<JsonObject> search(String keyword);

    Future<JsonObject> song(String path);

    Future<List<String>> songPaths();

    Future<Void> removeSongs(List<String> paths);

    Future<Void> clearDatabase();

    Future<Void> close();
}
