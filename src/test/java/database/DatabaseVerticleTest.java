package database;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import models.Album;
import models.AlbumData;
import models.SongData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static config.ServerConfig.DATABASE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(VertxExtension.class)
class DatabaseVerticleTest {

    private DatabaseService databaseService;
    private Path tempDbPath;

    @BeforeEach
    void prepared(Vertx vertx, VertxTestContext context) throws IOException {
        final var testDBPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.sqlite")).getPath();
        tempDbPath = Files.createTempFile(UUID.randomUUID() + "_db", ".db");
        Files.copy(Paths.get(testDBPath), tempDbPath, StandardCopyOption.REPLACE_EXISTING);

        final JsonObject config = new JsonObject().put(DATABASE_CONFIG, tempDbPath.toString());

        databaseService = DatabaseService.create(vertx, config);
        context.completeNow();
    }


    @AfterEach
    void tearDown() throws IOException {
        if (tempDbPath != null && Files.exists(tempDbPath)) {
            Files.delete(tempDbPath);
        }
    }

    @Test
    void verifyGetAlbums(Vertx vertx, VertxTestContext context) {
        databaseService.albums()
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(11, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/albums.json").toString().trim(),
                                        new JsonArray(result.stream()
                                                            .map(Album::toJson)
                                                            .toList())
                                                .encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyGetAlbum(Vertx vertx, VertxTestContext context) {
        databaseService.album(2037516188)
                       .onSuccess(album -> context.verify(() -> {
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/album.json").toString().trim(),
                                        album.toJson().encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyGenre(Vertx vertx, VertxTestContext context) {
        databaseService.genres()
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(7, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/genres.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyAlbumArtists(Vertx vertx, VertxTestContext context) {
        databaseService.albumArtists()
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(5, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/albumArtists.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyArtists(Vertx vertx, VertxTestContext context) {
        databaseService.artists()
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(6, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/artists.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyGetAlbumForGenre(Vertx vertx, VertxTestContext context) {
        databaseService.albumsForGenre(1471074189)
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(1, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/albumsForGenre.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyGetAlbumForAlbumArtists(Vertx vertx, VertxTestContext context) {
        databaseService.albumsForAlbumArtist(1034325051)
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(6, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/albumsForAlbumArtist.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyGetAlbumForArtists(Vertx vertx, VertxTestContext context) {
        databaseService.albumsForArtist(-1817732934)
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(2, result.size());
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/albumsForArtist.json").toString().trim(),
                                        new JsonArray(result).encode());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyScan(Vertx vertx, VertxTestContext context) {

        databaseService.scan(List.of(AlbumData.builder()
                                              .name("RANDOM_NAME")
                                              .date(LocalDate.now().toString())
                                              .artist("RANDOM_ARTIST")
                                              .songs(List.of(
                                                      SongData.builder()
                                                              .path("RANDOM_PATH")
                                                              .artist("RANDOM_ARTIST")
                                                              .album("RANDOM_ALBUM")
                                                              .albumArtist("RANDOM_ARTIST")
                                                              .title("RANDOM_TITLE")
                                                              .genre("RANDOM_GENRE")
                                                              .date(LocalDate.now().toString())
                                                              .composer("")
                                                              .performer("")
                                                              .disc(1)
                                                              .trackNum(1)
                                                              .duration(5)
                                                              .build()
                                              ))
                                              .atime(System.currentTimeMillis())
                                              .mtime(System.currentTimeMillis())
                                              .totalDuration(100)
                                              .build()))
                       .compose(__ -> Future.all(databaseService.albums(),
                                                 databaseService.genres(),
                                                 databaseService.artists(),
                                                 databaseService.albumArtists(),
                                                 databaseService.songPaths()))
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(List.of(12, 8, 7, 6, 151), result.list()
                                                                         .stream()
                                                                         .map(o -> ((List<?>) o).size())
                                                                         .collect(Collectors.toList()));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }


    @Test
    void verifySearch(Vertx vertx, VertxTestContext context) {
        databaseService.search("mu")
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(vertx.fileSystem().readFileBlocking("fixtures/db/search.json").toJsonObject(), result);
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifySongPathOperations(Vertx vertx, VertxTestContext context) {
        databaseService.songPaths()
                       .compose(result -> {
                           assertEquals(150, result.size());
                           Collections.shuffle(result);
                           return Future.all(databaseService.songsFromPath(List.of(result.getFirst())),
                                             databaseService.song(result.getFirst()));
                       })
                       .onSuccess(compositeFuture -> context.verify(() -> {
                           final List<?> results = compositeFuture.list();
                           final List<?> songs = (List<?>) results.getFirst();
                           assertEquals(1, songs.size());
                           assertTrue(songs.contains(results.getLast()));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifySongOperations(Vertx vertx, VertxTestContext context) {
        List<String> removeSongPaths = new ArrayList<>();
        databaseService.songPaths()
                       .compose(result -> {
                           assertEquals(150, result.size());
                           Collections.shuffle(result);
                           removeSongPaths.addAll(result.subList(0, 10));
                           return databaseService.removeSongs(removeSongPaths)
                                                 .compose(__ -> databaseService.songPaths());
                       })
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(140, result.size());
                           assertEquals(10, removeSongPaths.size());
                           assertTrue(Collections.disjoint(removeSongPaths, result));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyStat(Vertx vertx, VertxTestContext context) {
        databaseService.stats()
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(JsonObject.of("albums", 11, "songs", 150, "artists", 6), result);
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void verifyClearDatabase(Vertx vertx, VertxTestContext context) {
        databaseService.clearDatabase()
                       .compose(__ -> Future.all(databaseService.albums(),
                                                 databaseService.genres(),
                                                 databaseService.artists(),
                                                 databaseService.albumArtists()))
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(0, result.list()
                                                 .stream()
                                                 .mapToInt(o -> ((List<?>) o).size())
                                                 .sum());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }
}
