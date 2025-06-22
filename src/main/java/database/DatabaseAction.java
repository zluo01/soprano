package database;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

enum DatabaseAction {
    CLEAR_ALBUM_ARTISTS,
    CLEAR_ALBUMS,
    CLEAR_ARTISTS,
    CLEAR_GENRES,
    CLEAR_SONGS,
    CREATE_ALBUM_ARTISTS_TABLE,
    CREATE_ALBUMS_ADD_TIME_INDEX,
    CREATE_ALBUMS_TABLE,
    CREATE_ARTISTS_TABLE,
    CREATE_GENRES_TABLE,
    CREATE_SONGS_TABLE,
    GET_ALBUM,
    GET_ALBUM_ARTISTS,
    GET_ALBUMS_FOR_ALBUM_ARTIST,
    GET_ALBUMS,
    GET_ALBUMS_FOR_GENRE,
    GET_ARTISTS,
    GET_ALBUMS_FOR_ARTIST,
    INSERT_ALBUM,
    GET_GENRES,
    INSERT_ALBUM_ARTIST,
    INSERT_ARTIST,
    INSERT_GENRE,
    INSERT_SONG,
    GET_SONGS_DATA_FROM_PATHS,
    DELETE_SONGS_WITH_PATHS,
    CLEANUP_ALBUMS,
    GET_STATS,
    SEARCH_ALBUMS,
    SEARCH_ARTISTS,
    SEARCH_SONGS;

    private static final Map<DatabaseAction, String> QUERY_MAP;

    static {
        final ImmutableMap.Builder<DatabaseAction, String> builder = ImmutableMap.builder();
        for (DatabaseAction action : DatabaseAction.values()) {
            builder.put(action, getSQLContent(action));
        }
        QUERY_MAP = builder.build();
    }

    private static String getSQLContent(final DatabaseAction action) {
        final String sourcePath = "queries/" + action.name() + ".sql";
        try (InputStream stream = DatabaseAction.class.getClassLoader().getResourceAsStream(sourcePath);
             Scanner scanner = new Scanner(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException e) {
            throw new RuntimeException("Fail to load sql query from file " + sourcePath, e);
        }
    }

    String query() {
        if (QUERY_MAP.containsKey(this)) {
            return QUERY_MAP.get(this);
        }
        throw new IllegalArgumentException("Cannot find key: " + this.name());
    }
}
