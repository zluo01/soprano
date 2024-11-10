package server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.jdi.IntegerValue;
import database.DatabaseService;
import graphql.GraphQL;
import graphql.GraphQLException;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.Coercing;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;
import models.Album;
import playlists.PlaylistService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static enums.WorkerAction.SCAN_DIRECTORY;

final class GraphQLInitializer {
    private static final Cache<String, PreparsedDocumentEntry> QUERY_CACHE = Caffeine.newBuilder().maximumSize(1000).build();

    private GraphQLInitializer() {
    }

    public static GraphQL setup(final String schema,
                                final DatabaseService databaseService,
                                final PlaylistService playlistService,
                                final EventBus eventBus) {
        final PreparsedDocumentProvider preparsedCache = (executionInput, computeFunction) -> {
            Function<String, PreparsedDocumentEntry> mapCompute = key -> computeFunction.apply(executionInput);
            return QUERY_CACHE.get(executionInput.getQuery(), mapCompute);
        };

        final TypeDefinitionRegistry registry = new SchemaParser().parse(schema);
        final RuntimeWiring wiring = createWiring(databaseService, playlistService, eventBus);
        final GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(graphQLSchema)
                      .preparsedDocumentProvider(preparsedCache)
                      .instrumentation(new JsonObjectAdapter())
                      .build();
    }

    static RuntimeWiring createWiring(final DatabaseService databaseService,
                                      final PlaylistService playlistService,
                                      final EventBus eventBus) {
        final DataFetcher<CompletionStage<List<Album>>> albums = environment -> databaseService.albums().toCompletionStage();

        final DataFetcher<CompletionStage<Album>> album = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Album id is required");
            }
            return databaseService.album(id).toCompletionStage();
        };

        final DataFetcher<CompletionStage<List<JsonObject>>> genres = environment -> databaseService.genres().toCompletionStage();

        final DataFetcher<CompletionStage<List<JsonObject>>> albumsForGenre = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Genre id is required");
            }
            return databaseService.albumsForGenre(id).toCompletionStage();
        };

        final DataFetcher<CompletionStage<List<JsonObject>>> albumArtists = environment -> databaseService.albumArtists().toCompletionStage();

        final DataFetcher<CompletionStage<List<JsonObject>>> artists = environment -> databaseService.artists().toCompletionStage();

        final DataFetcher<CompletionStage<Boolean>> scan = environment -> {
            eventBus.publish(SCAN_DIRECTORY.name(), null);
            return CompletableFuture.completedFuture(true);
        };

        /*
         * Playlist Related
         */
        final DataFetcher<CompletionStage<List<JsonObject>>> playlists = environment -> playlistService.listPlaylists().toCompletionStage();

        final DataFetcher<CompletionStage<List<JsonObject>>> playlistSongs = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }
            return playlistService.playlistSongs(name).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> createPlaylist = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }
            return playlistService.createPlaylist(name).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> deletePlaylist = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }
            return playlistService.deletePlaylist(name).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> renamePlaylist = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }

            final String newName = environment.getArgument("newName");
            if (newName == null) {
                throw new IllegalArgumentException("Playlist newName is required");
            }

            return playlistService.renamePlaylist(name, newName).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> addSongToPlaylist = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }

            final String songPath = environment.getArgument("songPath");
            if (songPath == null) {
                throw new IllegalArgumentException("Song path is required");
            }

            return playlistService.addSongToPlaylist(name, songPath).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> deleteSongFromPlaylist = environment -> {
            final String name = environment.getArgument("name");
            if (name == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }

            final String songPath = environment.getArgument("songPath");
            if (songPath == null) {
                throw new IllegalArgumentException("Song path is required");
            }

            return playlistService.deleteSongFromPlaylist(name, songPath).toCompletionStage();
        };

        return RuntimeWiring.newRuntimeWiring()
                            .type("Query", builder -> builder.dataFetcher("Albums", albums))
                            .type("Query", builder -> builder.dataFetcher("Album", album))
                            .type("Query", builder -> builder.dataFetcher("Genres", genres))
                            .type("Query", builder -> builder.dataFetcher("Genre", albumsForGenre))
                            .type("Query", builder -> builder.dataFetcher("AlbumArtists", albumArtists))
                            .type("Query", builder -> builder.dataFetcher("Artists", artists))
                            .type("Query", builder -> builder.dataFetcher("Playlists", playlists))
                            .type("Query", builder -> builder.dataFetcher("Playlist", playlistSongs))
                            .type("Mutation", builder -> builder.dataFetcher("Scan", scan))
                            .type("Mutation", builder -> builder.dataFetcher("CreatePlaylist", createPlaylist))
                            .type("Mutation", builder -> builder.dataFetcher("DeletePlaylist", deletePlaylist))
                            .type("Mutation", builder -> builder.dataFetcher("RenamePlaylist", renamePlaylist))
                            .type("Mutation", builder -> builder.dataFetcher("AddSongToPlaylist", addSongToPlaylist))
                            .type("Mutation", builder -> builder.dataFetcher("DeleteSongFromPlaylist", deleteSongFromPlaylist))
                            .scalar(longScalar())
                            .build();
    }

    private static GraphQLScalarType longScalar() {
        return GraphQLScalarType.newScalar()
                                .name("Long")
                                .description("Java Long as scalar.")
                                .coercing(new LongScalar())
                                .build();
    }

    private static class LongScalar implements Coercing<Long, Long> {

        @Override
        public Long serialize(final Object input) {
            if (input instanceof Long) {
                return (Long) input;
            }
            throw new GraphQLException("Expected Long but got " + input.getClass().getSimpleName());
        }

        @Override
        public Long parseValue(final Object input) {
            if (input instanceof Number) {
                return ((Number) input).longValue();
            }
            throw new GraphQLException("Expected Long but got " + input.getClass().getSimpleName());
        }

        @Override
        public Long parseLiteral(final Object input) {
            if (input instanceof IntegerValue) {
                return ((IntegerValue) input).longValue();
            }
            throw new GraphQLException("Expected Long but got " + input.getClass().getSimpleName());
        }
    }
}
