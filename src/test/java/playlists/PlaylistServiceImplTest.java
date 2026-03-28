package playlists;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static helper.PathHelper.resolvePlaylistFilePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class PlaylistServiceImplTest {

    private PlaylistService playlistService;
    private final List<String> createdPlaylists = new ArrayList<>();

    @Mock
    private DatabaseService databaseService;
    private AutoCloseable closeable;

    @BeforeEach
    void setup(Vertx vertx) {
        closeable = MockitoAnnotations.openMocks(this);
        playlistService = new PlaylistServiceImpl(databaseService, vertx.fileSystem());
    }

    @AfterEach
    void tearDown() throws Exception {
        for (String name : createdPlaylists) {
            Files.deleteIfExists(Path.of(resolvePlaylistFilePath(name)));
        }
        closeable.close();
    }

    private String uniqueName() {
        final String name = "test_" + UUID.randomUUID();
        createdPlaylists.add(name);
        return name;
    }

    private void writePlaylistFile(String name, String content) throws IOException {
        Files.writeString(Path.of(resolvePlaylistFilePath(name)), content);
    }

    @Test
    void createPlaylist(Vertx vertx, VertxTestContext context) {
        final String name = uniqueName();
        playlistService.createPlaylist(name)
                       .onSuccess(result -> context.verify(() -> {
                           assertTrue(result);
                           assertTrue(Files.exists(Path.of(resolvePlaylistFilePath(name))));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void deletePlaylist(Vertx vertx, VertxTestContext context) {
        final String name = uniqueName();
        playlistService.createPlaylist(name)
                       .compose(__ -> playlistService.deletePlaylist(name))
                       .onSuccess(result -> context.verify(() -> {
                           assertTrue(result);
                           assertFalse(Files.exists(Path.of(resolvePlaylistFilePath(name))));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void renamePlaylist(Vertx vertx, VertxTestContext context) {
        final String name = uniqueName();
        final String newName = uniqueName();
        playlistService.createPlaylist(name)
                       .compose(__ -> playlistService.renamePlaylist(name, newName))
                       .onSuccess(result -> context.verify(() -> {
                           assertTrue(result);
                           assertFalse(Files.exists(Path.of(resolvePlaylistFilePath(name))));
                           assertTrue(Files.exists(Path.of(resolvePlaylistFilePath(newName))));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void renamePlaylistSameName(Vertx vertx, VertxTestContext context) {
        final String name = uniqueName();
        playlistService.createPlaylist(name)
                       .compose(__ -> playlistService.renamePlaylist(name, name))
                       .onSuccess(result -> context.verify(() -> {
                           assertTrue(result);
                           assertTrue(Files.exists(Path.of(resolvePlaylistFilePath(name))));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void addSongToPlaylist(Vertx vertx, VertxTestContext context) {
        final String name = uniqueName();
        playlistService.createPlaylist(name)
                       .compose(__ -> playlistService.addSongToPlaylist(name, "/music/song1.flac"))
                       .onSuccess(result -> context.verify(() -> {
                           try {
                               assertTrue(result);
                               final String content = Files.readString(Path.of(resolvePlaylistFilePath(name)));
                               assertTrue(content.contains("/music/song1.flac"));
                               context.completeNow();
                           } catch (IOException e) {
                               context.failNow(e);
                           }
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void addDuplicateSongToPlaylist(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        final String songPath = "/music/song1.flac";
        writePlaylistFile(name, songPath);

        playlistService.addSongToPlaylist(name, songPath)
                       .onSuccess(result -> context.verify(() -> {
                           try {
                               final String content = Files.readString(Path.of(resolvePlaylistFilePath(name)));
                               // should not duplicate the entry
                               assertEquals(1, content.lines().filter(l -> l.equals(songPath)).count());
                               context.completeNow();
                           } catch (IOException e) {
                               context.failNow(e);
                           }
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void addSongToPlaylistIgnoresBlankLines(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        writePlaylistFile(name, "/music/song1.flac\n\n\n/music/song2.flac\n");

        playlistService.addSongToPlaylist(name, "/music/song3.flac")
                       .onSuccess(result -> context.verify(() -> {
                           try {
                               final String content = Files.readString(Path.of(resolvePlaylistFilePath(name)));
                               final long nonBlankLines = content.lines().filter(l -> !l.isBlank()).count();
                               assertEquals(3, nonBlankLines);
                               assertFalse(content.lines().anyMatch(String::isBlank));
                               context.completeNow();
                           } catch (IOException e) {
                               context.failNow(e);
                           }
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void deleteSongFromPlaylist(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        writePlaylistFile(name, "/music/song1.flac\n/music/song2.flac\n/music/song3.flac");

        playlistService.deleteSongFromPlaylist(name, "/music/song2.flac")
                       .onSuccess(result -> context.verify(() -> {
                           try {
                               assertTrue(result);
                               final String content = Files.readString(Path.of(resolvePlaylistFilePath(name)));
                               assertFalse(content.contains("/music/song2.flac"));
                               assertTrue(content.contains("/music/song1.flac"));
                               assertTrue(content.contains("/music/song3.flac"));
                               context.completeNow();
                           } catch (IOException e) {
                               context.failNow(e);
                           }
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void playlistSongsPreservesOrder(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        writePlaylistFile(name, "/music/b.flac\n/music/a.flac\n/music/c.flac");

        when(databaseService.songsFromPath(any()))
                .thenAnswer(invocation -> {
                    final Collection<String> paths = invocation.getArgument(0);
                    final List<JsonObject> songs = paths.stream()
                                                        .map(p -> JsonObject.of("path", p, "name", Path.of(p).getFileName().toString()))
                                                        .toList();
                    return Future.succeededFuture(songs);
                });

        playlistService.playlistSongs(name)
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(3, result.size());
                           assertEquals("/music/b.flac", result.get(0).getString("path"));
                           assertEquals("/music/a.flac", result.get(1).getString("path"));
                           assertEquals("/music/c.flac", result.get(2).getString("path"));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void playlistSongsIgnoresBlankLines(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        writePlaylistFile(name, "/music/a.flac\n\n\n/music/b.flac\n");

        when(databaseService.songsFromPath(any()))
                .thenAnswer(invocation -> {
                    final Collection<String> paths = invocation.getArgument(0);
                    final List<JsonObject> songs = paths.stream()
                                                        .map(p -> JsonObject.of("path", p, "name", Path.of(p).getFileName().toString()))
                                                        .toList();
                    return Future.succeededFuture(songs);
                });

        playlistService.playlistSongs(name)
                       .onSuccess(result -> context.verify(() -> {
                           assertEquals(2, result.size());
                           assertEquals("/music/a.flac", result.get(0).getString("path"));
                           assertEquals("/music/b.flac", result.get(1).getString("path"));
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }

    @Test
    void playlistSongsEmptyFile(Vertx vertx, VertxTestContext context) throws IOException {
        final String name = uniqueName();
        writePlaylistFile(name, "");

        playlistService.playlistSongs(name)
                       .onSuccess(result -> context.verify(() -> {
                           verify(databaseService, never()).songsFromPath(any());
                           context.completeNow();
                       }))
                       .onFailure(context::failNow);
    }
}
