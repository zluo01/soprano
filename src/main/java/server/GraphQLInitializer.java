package server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import database.DatabaseService;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import io.vertx.ext.web.handler.graphql.instrumentation.VertxFutureAdapter;
import models.Album;
import org.reactivestreams.Publisher;
import player.PlayerService;
import playlists.PlaylistService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static enums.WorkerAction.DATABASE_UPDATE;
import static enums.WorkerAction.SCAN_DIRECTORY;
import static enums.WorkerAction.UPDATE_DIRECTORY;

final class GraphQLInitializer {
    private static final Cache<String, PreparsedDocumentEntry> QUERY_CACHE = Caffeine.newBuilder().maximumSize(1000).build();

    private GraphQLInitializer() {
    }

    public static GraphQL setup(final String schema,
                                final DatabaseService databaseService,
                                final PlaylistService playlistService,
                                final PlayerService playerService,
                                final EventBus eventBus) {
        final PreparsedDocumentProvider preparsedCache = (executionInput, computeFunction) -> {
            Function<String, PreparsedDocumentEntry> mapCompute = key -> computeFunction.apply(executionInput);
            return CompletableFuture.completedFuture(QUERY_CACHE.get(executionInput.getQuery(), mapCompute));
        };

        final TypeDefinitionRegistry registry = new SchemaParser().parse(schema);
        final RuntimeWiring wiring = createWiring(databaseService, playlistService, playerService, eventBus);
        final GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(registry, wiring);

        final List<Instrumentation> intrumentationList = List.of(new JsonObjectAdapter(), VertxFutureAdapter.create());
        final ChainedInstrumentation chainedInstrumentation = new ChainedInstrumentation(intrumentationList);
        return GraphQL.newGraphQL(graphQLSchema)
                      .preparsedDocumentProvider(preparsedCache)
                      .instrumentation(chainedInstrumentation)
                      .build();
    }

    static RuntimeWiring createWiring(final DatabaseService databaseService,
                                      final PlaylistService playlistService,
                                      final PlayerService playerService,
                                      final EventBus eventBus) {
        final DataFetcher<Future<List<Album>>> albums = environment -> databaseService.albums();

        final DataFetcher<Future<Album>> album = environment -> {
            final int id = extractField(environment, "id");
            return databaseService.album(id);
        };

        final DataFetcher<Future<List<JsonObject>>> genres = environment -> databaseService.genres();

        final DataFetcher<Future<List<JsonObject>>> albumsForGenre = environment -> {
            final int id = extractField(environment, "id");
            return databaseService.albumsForGenre(id);
        };

        final DataFetcher<Future<List<JsonObject>>> albumArtists = environment -> databaseService.albumArtists();

        final DataFetcher<Future<List<JsonObject>>> albumsForAlbumArtists = environment -> {
            final int id = extractField(environment, "id");
            return databaseService.albumsForAlbumArtist(id);
        };

        final DataFetcher<Future<List<JsonObject>>> artists = environment -> databaseService.artists();

        final DataFetcher<Future<List<JsonObject>>> albumsForArtist = environment -> {
            final int id = extractField(environment, "id");
            return databaseService.albumsForArtist(id);
        };

        final DataFetcher<Future<Boolean>> update = environment -> {
            eventBus.publish(UPDATE_DIRECTORY.name(), null);
            return Future.succeededFuture(true);
        };

        final DataFetcher<Future<Boolean>> build = environment -> {
            eventBus.publish(SCAN_DIRECTORY.name(), null);
            return Future.succeededFuture(true);
        };

        final DataFetcher<Publisher<Boolean>> onDatabaseUpdate = environment -> {
            final PublishProcessor<Boolean> processor = PublishProcessor.create();
            final var consumer = eventBus.<Boolean>consumer(DATABASE_UPDATE.name(), message -> {
                processor.onNext(message.body());
            });

            return processor.doOnCancel(consumer::unregister);
        };

        final DataFetcher<Future<JsonObject>> stats = environment -> databaseService.stats();

        final DataFetcher<Future<JsonObject>> search = environment -> {
            final String key = extractField(environment, "key");
            return databaseService.search(key);
        };

        final var wiringBuilder = RuntimeWiring.newRuntimeWiring();

        initializeSongOperations(wiringBuilder, databaseService);
        initializePlaylistOperations(wiringBuilder, playlistService);
        initializePlayerOperations(wiringBuilder, playerService);
        return wiringBuilder
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Albums", albums))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Album", album))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Genres", genres))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("GenreAlbums", albumsForGenre))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("AlbumArtists", albumArtists))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("AlbumArtistAlbums", albumsForAlbumArtists))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Artists", artists))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("ArtistAlbums", albumsForArtist))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Stats", stats))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Search", search))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("Build", build))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("Update", update))
                .type(GraphqlOperationType.Subscription.name(), builder -> builder.dataFetcher("OnDatabaseUpdate", onDatabaseUpdate))
                .scalar(ExtendedScalars.GraphQLLong)
                .build();
    }


    private static void initializeSongOperations(final RuntimeWiring.Builder wiringBuilder, final DatabaseService databaseService) {
        final DataFetcher<Future<JsonObject>> song = environment -> {
            final String path = extractField(environment, "path");
            return databaseService.song(path);
        };

        final DataFetcher<Future<List<JsonObject>>> songs = environment -> {
            final List<String> paths = extractField(environment, "paths");
            return databaseService.songsFromPath(paths);
        };

        wiringBuilder.type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Song", song))
                     .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Songs", songs));
    }

    private static void initializePlaylistOperations(final RuntimeWiring.Builder wiringBuilder,
                                                     final PlaylistService playlistService) {
        final DataFetcher<Future<List<JsonObject>>> playlists = environment -> playlistService.listPlaylists();

        final DataFetcher<Future<List<JsonObject>>> playlistSongs = environment -> {
            final String name = extractField(environment, "name");
            return playlistService.playlistSongs(name);
        };

        final DataFetcher<Future<Boolean>> createPlaylist = environment -> {
            final String name = extractField(environment, "name");
            return playlistService.createPlaylist(name);
        };

        final DataFetcher<Future<Boolean>> deletePlaylist = environment -> {
            final String name = extractField(environment, "name");
            return playlistService.deletePlaylist(name);
        };

        final DataFetcher<Future<Boolean>> renamePlaylist = environment -> {
            final String name = extractField(environment, "name");
            final String newName = extractField(environment, "newName");
            return playlistService.renamePlaylist(name, newName);
        };

        final DataFetcher<Future<Boolean>> addSongToPlaylist = environment -> {
            final String name = extractField(environment, "name");
            final String songPath = extractField(environment, "songPath");
            return playlistService.addSongToPlaylist(name, songPath);
        };

        final DataFetcher<Future<Boolean>> deleteSongFromPlaylist = environment -> {
            final String name = extractField(environment, "name");
            final String songPath = extractField(environment, "songPath");
            return playlistService.deleteSongFromPlaylist(name, songPath);
        };

        wiringBuilder
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("Playlists", playlists))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("PlaylistSongs", playlistSongs))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("CreatePlaylist", createPlaylist))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("DeletePlaylist", deletePlaylist))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("RenamePlaylist", renamePlaylist))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("AddSongToPlaylist", addSongToPlaylist))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("DeleteSongFromPlaylist", deleteSongFromPlaylist));
    }

    private static void initializePlayerOperations(final RuntimeWiring.Builder wiringBuilder,
                                                   final PlayerService playerService) {
        final DataFetcher<Future<List<JsonObject>>> songsInQueue = environment -> playerService.songsInQueue();

        final DataFetcher<Future<JsonObject>> playbackStatus = environment -> playerService.playbackStatus();

        final DataFetcher<Future<Integer>> playSong = environment -> {
            final String songPath = extractField(environment, "songPath");
            return playerService.playSong(songPath);
        };

        final DataFetcher<Future<Integer>> playPlaylist = environment -> {
            final String playlistName = extractField(environment, "playlistName");
            return playerService.playPlaylist(playlistName);
        };

        final DataFetcher<Future<Integer>> playAlbum = environment -> {
            final int id = extractField(environment, "id");
            return playerService.playAlbum(id);
        };

        final DataFetcher<Future<Integer>> pauseSong = environment -> playerService.pauseSong();
        final DataFetcher<Future<Integer>> nextSong = environment -> playerService.nextSong();
        final DataFetcher<Future<Integer>> prevSong = environment -> playerService.prevSong();

        final DataFetcher<Future<Integer>> toggleLoop = environment -> playerService.cycleRepeatMode();

        final DataFetcher<Future<Integer>> playSongInQueueAtPosition = environment -> {
            final int position = extractField(environment, "position");
            return playerService.playSongInQueueAtPosition(position);
        };

        final DataFetcher<Future<Integer>> addSongsToQueue = environment -> {
            final List<String> songPaths = extractField(environment, "songPaths");
            return playerService.addSongsToQueue(songPaths);
        };

        final DataFetcher<Future<Integer>> removeSongFromQueue = environment -> {
            final int position = extractField(environment, "position");
            return playerService.removeSongFromQueue(position);
        };

        final DataFetcher<Future<Integer>> clearQueue = environment -> playerService.clearQueue();

        wiringBuilder
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("SongsInQueue", songsInQueue))
                .type(GraphqlOperationType.Query.name(), builder -> builder.dataFetcher("PlaybackStatus", playbackStatus))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PlaySong", playSong))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PlayPlaylist", playPlaylist))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PlayAlbum", playAlbum))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PauseSong", pauseSong))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("NextSong", nextSong))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PrevSong", prevSong))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("ToggleLoop", toggleLoop))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("PlaySongInQueueAtPosition", playSongInQueueAtPosition))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("AddSongsToQueue", addSongsToQueue))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("RemoveSongFromQueue", removeSongFromQueue))
                .type(GraphqlOperationType.Mutation.name(), builder -> builder.dataFetcher("ClearQueue", clearQueue));
    }

    private static <T> T extractField(final DataFetchingEnvironment env, final String key) {
        final T field = env.getArgument(key);
        if (field == null) {
            throw new IllegalArgumentException(key + " is required.");
        }
        return field;
    }

    enum GraphqlOperationType {
        Query,
        Mutation,
        Subscription
    }
}
