package database;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import models.Album;
import models.AlbumData;

import java.util.List;

@ProxyGen
@VertxGen
public interface DatabaseService {
    @GenIgnore
    static DatabaseService create(JDBCPool pool) {
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

    Future<List<JsonObject>> albumsForAlbumArtists(int id);

    Future<List<JsonObject>> artists();

    Future<List<JsonObject>> albumsForArtists(int id);

    Future<Void> scan(List<AlbumData> albums);

    Future<List<JsonObject>> songsFromPath(List<String> paths);

    Future<JsonObject> stats();

    Future<JsonObject> search(String keyword);

    Future<JsonObject> song(String path);
}
