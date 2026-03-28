package server;

import database.DatabaseService;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import player.PlayerService;
import playlists.PlaylistService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static config.ServerConfig.DATABASE_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class WebServerVerticleTest {

    @Mock
    private PlaylistService playlistService;
    @Mock
    private PlayerService playerService;

    private GraphQL graphQL;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp(Vertx vertx) {
        closeable = MockitoAnnotations.openMocks(this);

        final var testDBPath = Objects.requireNonNull(getClass().getClassLoader().getResource("test.sqlite")).getPath();
        final DatabaseService databaseService = DatabaseService.create(vertx, new JsonObject().put(DATABASE_CONFIG, testDBPath));

        final String schema = vertx.fileSystem().readFileBlocking("schemas/main.graphql").toString();
        final TypeDefinitionRegistry registry = new SchemaParser().parse(schema);
        final RuntimeWiring wiring = GraphQLInitializer.createWiring(
                databaseService, playlistService, playerService, vertx.eventBus());
        final GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(registry, wiring);

        final List<Instrumentation> instrumentationList = List.of(new JsonObjectAdapter(), VertxFutureAdapter.create());
        graphQL = GraphQL.newGraphQL(graphQLSchema)
                         .instrumentation(new ChainedInstrumentation(instrumentationList))
                         .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @SuppressWarnings("unchecked")
    private <T> T executeQuery(String query) {
        final ExecutionResult result = graphQL.execute(ExecutionInput.newExecutionInput().query(query).build());
        assertTrue(result.getErrors().isEmpty(), "GraphQL errors: " + result.getErrors());
        return (T) result.getData();
    }

    @SuppressWarnings("unchecked")
    private <T> T executeQuery(String query, Map<String, Object> variables) {
        final ExecutionResult result = graphQL.execute(
                ExecutionInput.newExecutionInput().query(query).variables(variables).build());
        assertTrue(result.getErrors().isEmpty(), "GraphQL errors: " + result.getErrors());
        return (T) result.getData();
    }

    @Test
    void queryAlbums(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ Albums { id name artist date totalDuration songs { name artists path disc trackNum duration } } }");
        List<Map<String, Object>> albums = (List<Map<String, Object>>) data.get("Albums");
        assertEquals(11, albums.size());

        // verify one album's parsed data
        final var album = albums.stream()
                .filter(a -> a.get("id").equals(2037516188))
                .findFirst().orElseThrow();
        assertEquals("KID A MNESIA", album.get("name"));
        assertEquals("Radiohead", album.get("artist"));
        assertEquals("2021-11-05", album.get("date"));
        assertEquals(7711, album.get("totalDuration"));

        final var songs = (List<Map<String, Object>>) album.get("songs");
        assertEquals(34, songs.size());
        assertEquals("Everything in Its Right Place", songs.getFirst().get("name"));
        assertEquals(1, songs.getFirst().get("disc"));
        assertEquals(1, songs.getFirst().get("trackNum"));
        context.completeNow();
    }

    @Test
    void queryAlbumById(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ Album(id: 2037516188) { id name artist date totalDuration songs { name artists path disc trackNum duration } } }");
        Map<String, Object> album = (Map<String, Object>) data.get("Album");
        assertEquals(2037516188, album.get("id"));
        assertEquals("KID A MNESIA", album.get("name"));
        assertEquals("Radiohead", album.get("artist"));
        assertEquals(7711, album.get("totalDuration"));

        final var songs = (List<Map<String, Object>>) album.get("songs");
        assertEquals(34, songs.size());
        // verify ordering by disc then track number
        assertEquals(1, songs.getFirst().get("disc"));
        assertEquals(1, songs.getFirst().get("trackNum"));
        assertEquals("Everything in Its Right Place", songs.getFirst().get("name"));
        context.completeNow();
    }

    @Test
    void queryGenres(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery("{ Genres { id name albumCount } }");
        List<Map<String, Object>> genres = (List<Map<String, Object>>) data.get("Genres");
        assertEquals(7, genres.size());

        final var pop = genres.stream()
                .filter(g -> g.get("name").equals("Pop"))
                .findFirst().orElseThrow();
        assertNotNull(pop.get("id"));
        assertTrue((int) pop.get("albumCount") > 0);
        context.completeNow();
    }

    @Test
    void queryGenreAlbums(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ GenreAlbums(id: 1471074189) { id name artist } }");
        List<Map<String, Object>> albums = (List<Map<String, Object>>) data.get("GenreAlbums");
        assertEquals(1, albums.size());
        assertNotNull(albums.getFirst().get("name"));
        context.completeNow();
    }

    @Test
    void queryAlbumArtists(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery("{ AlbumArtists { id name albumCount } }");
        List<Map<String, Object>> albumArtists = (List<Map<String, Object>>) data.get("AlbumArtists");
        assertEquals(5, albumArtists.size());
        context.completeNow();
    }

    @Test
    void queryAlbumArtistAlbums(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ AlbumArtistAlbums(id: 1034325051) { id name artist } }");
        List<Map<String, Object>> albums = (List<Map<String, Object>>) data.get("AlbumArtistAlbums");
        assertEquals(6, albums.size());
        context.completeNow();
    }

    @Test
    void queryArtists(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery("{ Artists { id name albumCount } }");
        List<Map<String, Object>> artists = (List<Map<String, Object>>) data.get("Artists");
        assertEquals(6, artists.size());
        context.completeNow();
    }

    @Test
    void queryArtistAlbums(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ ArtistAlbums(id: -1817732934) { id name artist } }");
        List<Map<String, Object>> albums = (List<Map<String, Object>>) data.get("ArtistAlbums");
        assertEquals(2, albums.size());
        context.completeNow();
    }

    @Test
    void queryStats(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery("{ Stats { albums songs artists } }");
        Map<String, Object> stats = (Map<String, Object>) data.get("Stats");
        assertEquals(11, stats.get("albums"));
        assertEquals(150, stats.get("songs"));
        assertEquals(6, stats.get("artists"));
        context.completeNow();
    }

    @Test
    void querySearch(Vertx vertx, VertxTestContext context) {
        Map<String, Object> data = executeQuery(
                "{ Search(key: \"mu\") { albums { id name } songs { name path } artists { id name } } }");
        Map<String, Object> search = (Map<String, Object>) data.get("Search");
        assertNotNull(search.get("albums"));
        assertNotNull(search.get("songs"));
        assertNotNull(search.get("artists"));

        final var artists = (List<Map<String, Object>>) search.get("artists");
        assertTrue(artists.stream().anyMatch(a -> ((String) a.get("name")).contains("Mu")));
        context.completeNow();
    }

    @Test
    void querySong(Vertx vertx, VertxTestContext context) {
        final String path = "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-01-Muse-Uprising.flac";
        Map<String, Object> data = executeQuery(
                "query($p: String!) { Song(path: $p) { name artists albumId album path disc trackNum duration } }",
                Map.of("p", path));
        Map<String, Object> song = (Map<String, Object>) data.get("Song");
        assertEquals("Uprising", song.get("name"));
        assertEquals("Muse", song.get("artists"));
        assertEquals(627123027, song.get("albumId"));
        assertEquals("The Resistance", song.get("album"));
        assertEquals(1, song.get("disc"));
        assertEquals(1, song.get("trackNum"));
        assertEquals(303, song.get("duration"));
        context.completeNow();
    }

    @Test
    void querySongs(Vertx vertx, VertxTestContext context) {
        final List<String> paths = List.of(
                "/home/a/Music/Music/Muse/[Hi-Res][Muse]The Resistance/01-01-Muse-Uprising.flac",
                "/home/a/Music/Music/0024522628.flac");
        Map<String, Object> data = executeQuery(
                "query($paths: [String!]!) { Songs(paths: $paths) { name path } }",
                Map.of("paths", paths));
        List<Map<String, Object>> songs = (List<Map<String, Object>>) data.get("Songs");
        assertEquals(2, songs.size());
        context.completeNow();
    }

    @Test
    void queryPlaylists(Vertx vertx, VertxTestContext context) {
        when(playlistService.listPlaylists()).thenReturn(Future.succeededFuture(
                List.of(JsonObject.of("name", "MyPlaylist", "modifiedTime", 1000L, "songCount", 3))));

        Map<String, Object> data = executeQuery("{ Playlists { name songCount } }");
        List<Map<String, Object>> playlists = (List<Map<String, Object>>) data.get("Playlists");
        assertEquals(1, playlists.size());
        assertEquals("MyPlaylist", playlists.getFirst().get("name"));
        assertEquals(3, playlists.getFirst().get("songCount"));
        context.completeNow();
    }

    @Test
    void mutationCreatePlaylist(Vertx vertx, VertxTestContext context) {
        when(playlistService.createPlaylist("NewList")).thenReturn(Future.succeededFuture(true));

        Map<String, Object> data = executeQuery("mutation { CreatePlaylist(name: \"NewList\") }");
        assertEquals(true, data.get("CreatePlaylist"));
        verify(playlistService).createPlaylist("NewList");
        context.completeNow();
    }

    @Test
    void mutationDeletePlaylist(Vertx vertx, VertxTestContext context) {
        when(playlistService.deletePlaylist("OldList")).thenReturn(Future.succeededFuture(true));

        Map<String, Object> data = executeQuery("mutation { DeletePlaylist(name: \"OldList\") }");
        assertEquals(true, data.get("DeletePlaylist"));
        verify(playlistService).deletePlaylist("OldList");
        context.completeNow();
    }

    @Test
    void mutationRenamePlaylist(Vertx vertx, VertxTestContext context) {
        when(playlistService.renamePlaylist("Old", "New")).thenReturn(Future.succeededFuture(true));

        Map<String, Object> data = executeQuery("mutation { RenamePlaylist(name: \"Old\", newName: \"New\") }");
        assertEquals(true, data.get("RenamePlaylist"));
        verify(playlistService).renamePlaylist("Old", "New");
        context.completeNow();
    }

    @Test
    void mutationAddSongToPlaylist(Vertx vertx, VertxTestContext context) {
        when(playlistService.addSongToPlaylist("List", "/song.flac")).thenReturn(Future.succeededFuture(true));

        Map<String, Object> data = executeQuery(
                "mutation { AddSongToPlaylist(name: \"List\", songPath: \"/song.flac\") }");
        assertEquals(true, data.get("AddSongToPlaylist"));
        verify(playlistService).addSongToPlaylist("List", "/song.flac");
        context.completeNow();
    }

    @Test
    void mutationDeleteSongFromPlaylist(Vertx vertx, VertxTestContext context) {
        when(playlistService.deleteSongFromPlaylist("List", "/song.flac")).thenReturn(Future.succeededFuture(true));

        Map<String, Object> data = executeQuery(
                "mutation { DeleteSongFromPlaylist(name: \"List\", songPath: \"/song.flac\") }");
        assertEquals(true, data.get("DeleteSongFromPlaylist"));
        verify(playlistService).deleteSongFromPlaylist("List", "/song.flac");
        context.completeNow();
    }

    @Test
    void mutationPlaySong(Vertx vertx, VertxTestContext context) {
        when(playerService.playSong("/song.flac")).thenReturn(Future.succeededFuture(0));

        Map<String, Object> data = executeQuery("mutation { PlaySong(songPath: \"/song.flac\") }");
        assertEquals(0, data.get("PlaySong"));
        verify(playerService).playSong("/song.flac");
        context.completeNow();
    }

    @Test
    void mutationPauseSong(Vertx vertx, VertxTestContext context) {
        when(playerService.pauseSong()).thenReturn(Future.succeededFuture(0));

        Map<String, Object> data = executeQuery("mutation { PauseSong }");
        assertEquals(0, data.get("PauseSong"));
        verify(playerService).pauseSong();
        context.completeNow();
    }

    @Test
    void mutationClearQueue(Vertx vertx, VertxTestContext context) {
        when(playerService.clearQueue()).thenReturn(Future.succeededFuture(0));

        Map<String, Object> data = executeQuery("mutation { ClearQueue }");
        assertEquals(0, data.get("ClearQueue"));
        verify(playerService).clearQueue();
        context.completeNow();
    }

    @Test
    void queryPlaybackStatus(Vertx vertx, VertxTestContext context) {
        when(playerService.playbackStatus()).thenReturn(Future.succeededFuture(
                JsonObject.of("playing", false, "elapsed", 0, "loopId", 0)));

        Map<String, Object> data = executeQuery("{ PlaybackStatus { playing elapsed loopId } }");
        Map<String, Object> status = (Map<String, Object>) data.get("PlaybackStatus");
        assertEquals(false, status.get("playing"));
        assertEquals(0, status.get("elapsed"));
        context.completeNow();
    }

    @Test
    void querySongsInQueue(Vertx vertx, VertxTestContext context) {
        when(playerService.songsInQueue()).thenReturn(Future.succeededFuture(List.of()));

        Map<String, Object> data = executeQuery("{ SongsInQueue { name path } }");
        List<?> queue = (List<?>) data.get("SongsInQueue");
        assertTrue(queue.isEmpty());
        context.completeNow();
    }

    @Test
    void queryMissingArgumentReturnsError(Vertx vertx, VertxTestContext context) {
        final ExecutionResult result = graphQL.execute(
                ExecutionInput.newExecutionInput().query("{ Album { id } }").build());
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        context.completeNow();
    }
}
