package database;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import models.Album;
import models.AlbumData;

import java.util.List;

@ProxyGen
@VertxGen
public interface DatabaseService {
    @GenIgnore
    static DatabaseService create(Pool pool) {
        return new DatabaseServiceImpl(pool);
    }

    @GenIgnore
    static DatabaseService createProxy(Vertx vertx, String address) {
        return new DatabaseServiceVertxEBProxy(vertx, address);
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
}
