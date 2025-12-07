package player;

import database.DatabaseService;
import helper.ServiceHelper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import player.base.AudioPlayer;
import player.base.PlaybackStatus;
import playlists.PlaylistService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static config.ServerConfig.DATABASE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class PlayerVerticleTest {

    private PlayerService playerService;
    private Path tempDbPath;

    @Mock
    private PlaylistService playlistService;
    @Mock
    private AudioPlayer audioPlayer;
    @Mock
    private AutoCloseable closeable;

    @BeforeEach
    void prepared(Vertx vertx, VertxTestContext context) throws IOException {
        closeable = MockitoAnnotations.openMocks(this);

        Mockito.when(audioPlayer.play(Mockito.anyString())).thenReturn(Future.succeededFuture(0));
        Mockito.when(audioPlayer.stop()).thenReturn(Future.succeededFuture(0));
        Mockito.doNothing().when(audioPlayer).startMonitor(Mockito.any());

        final var testDBPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("test.sqlite")).getPath();
        tempDbPath = Files.createTempFile(UUID.randomUUID() + "_db", ".db");
        Files.copy(Paths.get(testDBPath), tempDbPath, StandardCopyOption.REPLACE_EXISTING);

        final JsonObject config = new JsonObject().put(DATABASE_CONFIG, tempDbPath.toString());

        final DatabaseService databaseService = DatabaseService.create(vertx, config);
        vertx.deployVerticle(new PlayerVerticle(databaseService, playlistService, audioPlayer),
                             new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
                                                    .setWorkerPoolName("Player")
                                                    .setConfig(config))
             .onSuccess(__ -> {
                 playerService = ServiceHelper.createServiceProxy(vertx, PlayerVerticle.class, PlayerService.class);
                 context.completeNow();
             })
             .onFailure(context::failNow);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        if (tempDbPath != null && Files.exists(tempDbPath)) {
            Files.delete(tempDbPath);
        }
    }

    @Test
    void verifyPlaySong(Vertx vertx, VertxTestContext context) {
        final var path = "/home/a/Music/Music/Radiohead/[Hi-Res][Radiohead]OK Computer OKNOTOK 1997 2017/106 - Karma Police (Remastered).wav";
        playerService.playSong(path)
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.eq(path));
                         assertEquals(1, songs.size());
                         assertEquals(path, songs.getFirst().getString("path"));
                         assertEquals("Karma Police", songs.getFirst().getString("name"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyPlayPlaylist(Vertx vertx, VertxTestContext context) {
        final var expectedSongs = List.of(
                JsonObject.of(
                        "name", "Karma Police",
                        "path", "/home/a/Music/Music/Radiohead/[Hi-Res][Radiohead]OK Computer OKNOTOK 1997 2017/106 - Karma Police (Remastered).wav"
                ),
                JsonObject.of(
                        "name", "Pyramid Strings",
                        "path", "/home/a/Music/Music/Radiohead/[Radiohead]KID A MNESIA/Radiohead - KID A MNESIA - 31 Pyramid Strings.flac"
                ),
                JsonObject.of(
                        "name", "Remember Summer Days",
                        "path", "/home/a/Music/Music/0024522628.flac"
                )
        );

        Mockito.when(playlistService.playlistSongs(Mockito.anyString()))
               .thenReturn(Future.succeededFuture(expectedSongs));
        playerService.playPlaylist("RANDOM_PLAYLIST")
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.eq(expectedSongs.getFirst().getString("path")));
                         assertEquals(expectedSongs.size(), songs.size());
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyPlayAlbum(Vertx vertx, VertxTestContext context) {
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-01-Muse-Uprising.flac";
        playerService.playAlbum(627123027)
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.eq(path));
                         assertEquals(11, songs.size());
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyNextSong(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-02-Muse-Resistance.flac";
        playerService.playAlbum(627123027)
                     .compose(__ -> playerService.nextSong())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(2)).play(Mockito.anyString());
                         assertTrue(status.getBoolean("playing"));
                         assertEquals(0, status.getInteger("loopId"));
                         assertEquals(path, status.getJsonObject("song").getString("path"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyNextSongReachEndWhenRepeatModeIsNone(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        playerService.playAlbum(1375336646)
                     .compose(__ -> playerService.nextSong())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.anyString());
                         assertFalse(status.getBoolean("playing"));
                         assertEquals(0, status.getInteger("loopId"));
                         assertFalse(status.containsKey("song"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyNextSongReachEndWhenRepeatModeIsSingle(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        final var path = "/home/a/Music/Music/0024522628.flac";
        playerService.playAlbum(1375336646)
                     .compose(__ -> playerService.cycleRepeatMode())
                     .compose(__ -> playerService.nextSong())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(2)).play(Mockito.eq(path));
                         assertTrue(status.getBoolean("playing"));
                         assertEquals(1, status.getInteger("loopId"));
                         assertEquals(path, status.getJsonObject("song").getString("path"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyNextSongReachEndWhenRepeatModeIsAll(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        final var path = "/home/a/Music/Music/0024522628.flac";
        playerService.playAlbum(1375336646)
                     .compose(__ -> playerService.cycleRepeatMode())
                     .compose(__ -> playerService.cycleRepeatMode())
                     .compose(__ -> playerService.nextSong())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(2)).play(Mockito.eq(path));
                         assertTrue(status.getBoolean("playing"));
                         assertEquals(2, status.getInteger("loopId"));
                         assertEquals(path, status.getJsonObject("song").getString("path"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyPrevSong(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-01-Muse-Uprising.flac";
        playerService.playAlbum(627123027)
                     .compose(__ -> playerService.prevSong())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(2)).play(Mockito.eq(path));
                         assertTrue(status.getBoolean("playing"));
                         assertEquals(0, status.getInteger("loopId"));
                         assertEquals(path, status.getJsonObject("song").getString("path"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyPlaySongAtPosition(Vertx vertx, VertxTestContext context) {
        Mockito.when(audioPlayer.playbackStatus()).thenReturn(Future.succeededFuture(new PlaybackStatus(true, 1)));
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-03-Muse-Undisclosed_Desires.flac";
        playerService.playAlbum(627123027)
                     .compose(__ -> playerService.playSongInQueueAtPosition(2))
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(2)).play(Mockito.anyString());
                         assertTrue(status.getBoolean("playing"));
                         assertEquals(0, status.getInteger("loopId"));
                         assertEquals(path, status.getJsonObject("song").getString("path"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyAddSongToQueue(Vertx vertx, VertxTestContext context) {
        final var expectedSongs = List.of(
                "/home/a/Music/Music/Radiohead/[Hi-Res][Radiohead]OK Computer OKNOTOK 1997 2017/106 - Karma Police (Remastered).wav",
                "/home/a/Music/Music/Radiohead/[Radiohead]KID A MNESIA/Radiohead - KID A MNESIA - 31 Pyramid Strings.flac",
                "/home/a/Music/Music/0024522628.flac"
        );
        playerService.playAlbum(627123027)
                     .compose(__ -> playerService.addSongsToQueue(expectedSongs))
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.anyString());
                         assertEquals(14, songs.size());
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyRemoveSong(Vertx vertx, VertxTestContext context) {
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-03-Muse-Undisclosed_Desires.flac";
        playerService.playAlbum(627123027)
                     .compose(__ -> playerService.removeSongFromQueue(2))
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.anyString());
                         assertEquals(10, songs.size());
                         assertFalse(songs.stream()
                                          .map(o -> o.getString("path"))
                                          .anyMatch(s -> s.equals(path)));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyWhenClearQueueForPlaybackStatus(Vertx vertx, VertxTestContext context) {
        playerService.playAlbum(627123027)
                     .flatMap(__ -> playerService.clearQueue())
                     .flatMap(__ -> playerService.playbackStatus())
                     .onSuccess(status -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.anyString());
                         assertFalse(status.getBoolean("playing"));
                         assertEquals(0, status.getInteger("loopId"));
                         assertFalse(status.containsKey("song"));
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }

    @Test
    void verifyWhenClearQueueForSongInQueue(Vertx vertx, VertxTestContext context) {
        playerService.playAlbum(627123027)
                     .flatMap(__ -> playerService.clearQueue())
                     .flatMap(__ -> playerService.songsInQueue())
                     .onSuccess(songs -> context.verify(() -> {
                         Mockito.verify(audioPlayer, Mockito.times(1)).play(Mockito.anyString());
                         assertEquals(0, songs.size());
                         context.completeNow();
                     }))
                     .onFailure(context::failNow);
    }
}
