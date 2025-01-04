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
import player.PlayerService;
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
                                final PlayerService playerService,
                                final EventBus eventBus) {
        final PreparsedDocumentProvider preparsedCache = (executionInput, computeFunction) -> {
            Function<String, PreparsedDocumentEntry> mapCompute = key -> computeFunction.apply(executionInput);
            return QUERY_CACHE.get(executionInput.getQuery(), mapCompute);
        };

        final TypeDefinitionRegistry registry = new SchemaParser().parse(schema);
        final RuntimeWiring wiring = createWiring(databaseService, playlistService, playerService, eventBus);
        final GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        return GraphQL.newGraphQL(graphQLSchema)
                      .preparsedDocumentProvider(preparsedCache)
                      .instrumentation(new JsonObjectAdapter())
                      .build();
    }

    static RuntimeWiring createWiring(final DatabaseService databaseService,
                                      final PlaylistService playlistService,
                                      final PlayerService playerService,
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

        final DataFetcher<CompletionStage<List<JsonObject>>> albumsForAlbumArtists = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Album Artist id is required");
            }
            return databaseService.albumsForAlbumArtists(id).toCompletionStage();
        };

        final DataFetcher<CompletionStage<List<JsonObject>>> artists = environment -> databaseService.artists().toCompletionStage();

        final DataFetcher<CompletionStage<List<JsonObject>>> albumsForArtist = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Artist id is required");
            }
            return databaseService.albumsForArtists(id).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Boolean>> build = environment -> {
            eventBus.publish(SCAN_DIRECTORY.name(), null);
            return CompletableFuture.completedFuture(true);
        };

        final DataFetcher<CompletionStage<JsonObject>> stats = environment -> databaseService.stats().toCompletionStage();

        final DataFetcher<CompletionStage<JsonObject>> search = environment -> {
            final String key = environment.getArgument("key");
            if (key == null) {
                throw new IllegalArgumentException("keyword is required");
            }

            return databaseService.search(key).toCompletionStage();
        };

        final var wiringBuilder = RuntimeWiring.newRuntimeWiring();

        initializeSongOperations(wiringBuilder, databaseService);
        initializePlaylistOperations(wiringBuilder, playlistService);
        initializePlayerOperations(wiringBuilder, playerService);
        return wiringBuilder
                .type("Query", builder -> builder.dataFetcher("Albums", albums))
                .type("Query", builder -> builder.dataFetcher("Album", album))
                .type("Query", builder -> builder.dataFetcher("Genres", genres))
                .type("Query", builder -> builder.dataFetcher("GenreAlbums", albumsForGenre))
                .type("Query", builder -> builder.dataFetcher("AlbumArtists", albumArtists))
                .type("Query", builder -> builder.dataFetcher("AlbumArtistAlbums", albumsForAlbumArtists))
                .type("Query", builder -> builder.dataFetcher("Artists", artists))
                .type("Query", builder -> builder.dataFetcher("ArtistAlbums", albumsForArtist))
                .type("Query", builder -> builder.dataFetcher("Stats", stats))
                .type("Query", builder -> builder.dataFetcher("Search", search))
                .type("Mutation", builder -> builder.dataFetcher("Build", build))

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

    private static void initializeSongOperations(final RuntimeWiring.Builder wiringBuilder, final DatabaseService databaseService) {
        final DataFetcher<CompletionStage<JsonObject>> song = environment -> {
            final String path = environment.getArgument("path");
            if (path == null) {
                throw new IllegalArgumentException("Path is required");
            }
            return databaseService.song(path).toCompletionStage();
        };

        final DataFetcher<CompletionStage<List<JsonObject>>> songs = environment -> {
            final List<String> paths = environment.getArgument("paths");
            if (paths == null) {
                throw new IllegalArgumentException("Paths are required");
            }
            return databaseService.songsFromPath(paths).toCompletionStage();
        };

        wiringBuilder.type("Query", builder -> builder.dataFetcher("Song", song))
                     .type("Query", builder -> builder.dataFetcher("Songs", songs));
    }

    private static void initializePlaylistOperations(final RuntimeWiring.Builder wiringBuilder,
                                                     final PlaylistService playlistService) {
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

        wiringBuilder
                .type("Query", builder -> builder.dataFetcher("Playlists", playlists))
                .type("Query", builder -> builder.dataFetcher("PlaylistSongs", playlistSongs))
                .type("Mutation", builder -> builder.dataFetcher("CreatePlaylist", createPlaylist))
                .type("Mutation", builder -> builder.dataFetcher("DeletePlaylist", deletePlaylist))
                .type("Mutation", builder -> builder.dataFetcher("RenamePlaylist", renamePlaylist))
                .type("Mutation", builder -> builder.dataFetcher("AddSongToPlaylist", addSongToPlaylist))
                .type("Mutation", builder -> builder.dataFetcher("DeleteSongFromPlaylist", deleteSongFromPlaylist));
    }

    private static void initializePlayerOperations(final RuntimeWiring.Builder wiringBuilder,
                                                   final PlayerService playerService) {
        final DataFetcher<CompletionStage<List<JsonObject>>> songsInQueue = environment -> playerService.songsInQueue().toCompletionStage();

        final DataFetcher<CompletionStage<JsonObject>> playbackStatus = environment -> playerService.playbackStatus().toCompletionStage();

        final DataFetcher<CompletionStage<Integer>> playSong = environment -> {
            final String songPath = environment.getArgument("songPath");
            if (songPath == null) {
                throw new IllegalArgumentException("Song path is required");
            }

            return playerService.playSong(songPath).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> playPlaylist = environment -> {
            final String playlistName = environment.getArgument("playlistName");
            if (playlistName == null) {
                throw new IllegalArgumentException("Playlist name is required");
            }

            return playerService.playPlaylist(playlistName).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> playAlbum = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Album id is required");
            }

            return playerService.playAlbum(id).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> pauseSong = environment -> playerService.pauseSong().toCompletionStage();
        final DataFetcher<CompletionStage<Integer>> nextSong = environment -> playerService.nextSong().toCompletionStage();
        final DataFetcher<CompletionStage<Integer>> prevSong = environment -> playerService.prevSong().toCompletionStage();

        final DataFetcher<CompletionStage<Integer>> toggleLoop = environment -> {
            final Integer id = environment.getArgument("id");
            if (id == null) {
                throw new IllegalArgumentException("Loop id is required");
            }

            return playerService.toggleLoop(id).toCompletionStage();
        };


        final DataFetcher<CompletionStage<Integer>> playSongInQueueAtPosition = environment -> {
            final Integer position = environment.getArgument("position");
            if (position == null) {
                throw new IllegalArgumentException("Song position in queue is required");
            }

            return playerService.playSongInQueueAtPosition(position).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> addSongsToQueue = environment -> {
            final List<String> songPaths = environment.getArgument("songPaths");
            if (songPaths == null) {
                throw new IllegalArgumentException("Song paths are required");
            }

            return playerService.addSongsToQueue(songPaths).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> removeSongFromQueue = environment -> {
            final Integer position = environment.getArgument("position");
            if (position == null) {
                throw new IllegalArgumentException("Song position in queue is required");
            }

            return playerService.removeSongFromQueue(position).toCompletionStage();
        };

        final DataFetcher<CompletionStage<Integer>> clearQueue = environment -> playerService.clearQueue().toCompletionStage();

        wiringBuilder
                .type("Query", builder -> builder.dataFetcher("SongsInQueue", songsInQueue))
                .type("Query", builder -> builder.dataFetcher("PlaybackStatus", playbackStatus))
                .type("Mutation", builder -> builder.dataFetcher("PlaySong", playSong))
                .type("Mutation", builder -> builder.dataFetcher("PlayPlaylist", playPlaylist))
                .type("Mutation", builder -> builder.dataFetcher("PlayAlbum", playAlbum))
                .type("Mutation", builder -> builder.dataFetcher("PauseSong", pauseSong))
                .type("Mutation", builder -> builder.dataFetcher("NextSong", nextSong))
                .type("Mutation", builder -> builder.dataFetcher("PrevSong", prevSong))
                .type("Mutation", builder -> builder.dataFetcher("ToggleLoop", toggleLoop))
                .type("Mutation", builder -> builder.dataFetcher("PlaySongInQueueAtPosition", playSongInQueueAtPosition))
                .type("Mutation", builder -> builder.dataFetcher("AddSongsToQueue", addSongsToQueue))
                .type("Mutation", builder -> builder.dataFetcher("RemoveSongFromQueue", removeSongFromQueue))
                .type("Mutation", builder -> builder.dataFetcher("ClearQueue", clearQueue));
    }
}
