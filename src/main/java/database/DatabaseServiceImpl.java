package database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import models.Album;
import models.AlbumData;
import models.Song;
import models.SongData;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


class DatabaseServiceImpl implements DatabaseService {

    private final Pool pool;

    DatabaseServiceImpl(final Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Void> initialization() {
        return pool.getConnection()
                   .compose(connection -> connection.begin()
                                                    .compose(transaction -> connection
                                                            .query(DatabaseAction.CREATE_ALBUMS_TABLE.query())
                                                            .execute()
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CREATE_SONGS_TABLE.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CREATE_GENRES_TABLE.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CREATE_ARTISTS_TABLE.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CREATE_ALBUMS_ADD_TIME_INDEX.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CREATE_ALBUM_ARTISTS_TABLE.query())
                                                                    .execute())
                                                            .compose(__ -> transaction.commit()))
                                                    .eventually((Supplier<Future<Void>>) connection::close));
    }

    @Override
    public Future<List<Album>> albums() {
        return pool.query(DatabaseAction.GET_ALBUMS.query())
                   .execute()
                   .map(rows -> {
                       final Map<Integer, Album> albums = new HashMap<>();
                       for (Row row : rows) {
                           final int id = row.getInteger("album_id");
                           albums.compute(id, (albumId, album) -> {
                               if (album == null) {
                                   album = Album.builder()
                                                .id(id)
                                                .name(row.getString("album_name"))
                                                .artist(row.getString("album_artist"))
                                                .songs(new ArrayList<>())
                                                .date(row.getString("album_date"))
                                                .addTime(row.getLong("album_add_time"))
                                                .modifiedTime(row.getLong("album_modified_time"))
                                                .totalDuration(row.getInteger("album_total_duration"))
                                                .build();
                               }
                               final var song = Song.builder()
                                                    .albumId(id)
                                                    .album(album.name())
                                                    .name(row.getString("song_name"))
                                                    .artists(row.getString("song_artists"))
                                                    .path(row.getString("song_path"))
                                                    .disc(row.getInteger("song_disc"))
                                                    .trackNum(row.getInteger("song_track_num"))
                                                    .duration(row.getInteger("song_duration"))
                                                    .build();

                               album.songs().add(song);
                               return album;
                           });
                       }
                       return new ArrayList<>(albums.values());
                   });
    }

    @Override
    public Future<Album> album(final int id) {
        return pool.preparedQuery(DatabaseAction.GET_ALBUM.query())
                   .execute(Tuple.of(id))
                   .map(rows -> {
                       Album.Builder albumBuilder = null;
                       final List<Song> songs = new ArrayList<>();
                       for (Row row : rows) {
                           if (albumBuilder == null) {
                               albumBuilder = Album.builder()
                                                   .id(id)
                                                   .name(row.getString("album_name"))
                                                   .artist(row.getString("album_artist"))
                                                   .date(row.getString("album_date"))
                                                   .addTime(row.getLong("album_add_time"))
                                                   .modifiedTime(row.getLong("album_modified_time"))
                                                   .totalDuration(row.getInteger(("album_total_duration")));
                           }
                           songs.add(Song.builder()
                                         .albumId(id)
                                         .album(row.getString("album_name"))
                                         .name(row.getString("song_name"))
                                         .artists(row.getString("song_artists"))
                                         .path(row.getString("song_path"))
                                         .disc(row.getInteger("song_disc"))
                                         .trackNum(row.getInteger("song_track_num"))
                                         .duration(row.getInteger("song_duration"))
                                         .build());
                       }
                       if (albumBuilder == null) {
                           throw new IllegalArgumentException("Rows should not be empty.");
                       }
                       return albumBuilder.songs(songs).build();
                   });
    }

    @Override
    public Future<List<JsonObject>> genres() {
        return pool.query(DatabaseAction.GET_GENRES.query())
                   .execute()
                   .map(rows -> {
                       final List<JsonObject> genres = new ArrayList<>();
                       for (Row row : rows) {
                           genres.add(row.toJson());
                       }
                       return genres;
                   });
    }

    @Override
    public Future<List<JsonObject>> albumsForGenre(final int id) {
        return pool.preparedQuery(DatabaseAction.GET_ALBUMS_FOR_GENRE.query())
                   .execute(Tuple.of(id))
                   .map(rows -> {
                       final List<JsonObject> albums = new ArrayList<>();
                       for (Row row : rows) {
                           albums.add(row.toJson());
                       }
                       return albums;
                   });
    }

    @Override
    public Future<List<JsonObject>> albumArtists() {
        return pool.query(DatabaseAction.GET_ALBUM_ARTISTS.query())
                   .execute()
                   .map(rows -> {
                       final List<JsonObject> genres = new ArrayList<>();
                       for (Row row : rows) {
                           genres.add(row.toJson());
                       }
                       return genres;
                   });
    }

    @Override
    public Future<List<JsonObject>> albumsForAlbumArtist(final int id) {
        return pool.preparedQuery(DatabaseAction.GET_ALBUMS_FOR_ALBUM_ARTIST.query())
                   .execute(Tuple.of(id))
                   .map(rows -> {
                       final List<JsonObject> albums = new ArrayList<>();
                       for (Row row : rows) {
                           albums.add(row.toJson());
                       }
                       return albums;
                   });
    }

    @Override
    public Future<List<JsonObject>> artists() {
        return pool.query(DatabaseAction.GET_ARTISTS.query())
                   .execute()
                   .map(rows -> {
                       final List<JsonObject> genres = new ArrayList<>();
                       for (Row row : rows) {
                           genres.add(row.toJson());
                       }
                       return genres;
                   });
    }

    @Override
    public Future<List<JsonObject>> albumsForArtist(final int id) {
        return pool.preparedQuery(DatabaseAction.GET_ALBUMS_FOR_ARTIST.query())
                   .execute(Tuple.of(id))
                   .map(rows -> {
                       final List<JsonObject> albums = new ArrayList<>();
                       for (Row row : rows) {
                           albums.add(row.toJson());
                       }
                       return albums;
                   });
    }

    @Override
    public Future<Void> scan(final List<AlbumData> albums) {
        if (albums.isEmpty()) {
            return Future.succeededFuture();
        }
        final List<Tuple> albumQueryInput = new ArrayList<>();
        final List<Tuple> songQueryInput = new ArrayList<>();
        final List<Tuple> genreQueryInput = new ArrayList<>();
        final List<Tuple> artistsQueryInput = new ArrayList<>();
        final List<Tuple> albumArtistsQueryInput = new ArrayList<>();
        for (final AlbumData album : albums) {
            final int id = album.generateKey();
            albumQueryInput.add(Tuple.of(id, album.name(), album.date(), album.atime(), album.mtime(), album.totalDuration()));

            if (Strings.isNotBlank(album.artist())) {
                albumArtistsQueryInput.add(Tuple.of(album.artist().hashCode(), album.artist(), id));
            }

            for (final SongData song : album.songs()) {
                songQueryInput.add(Tuple.of(song.title(),
                                            song.artist(),
                                            id,
                                            song.path(),
                                            song.date(),
                                            song.genre(),
                                            song.composer(),
                                            song.performer(),
                                            song.disc(),
                                            song.trackNum(),
                                            song.duration(),
                                            song.mtime(),
                                            song.atime()));

                if (Strings.isNotBlank(song.genre())) {
                    genreQueryInput.add(Tuple.of(song.genre().hashCode(), song.genre(), id));
                }

                if (Strings.isNotBlank(song.artist())) {
                    artistsQueryInput.add(Tuple.of(song.artist().hashCode(), song.artist(), id));
                }
            }
        }
        return pool.getConnection()
                   .compose(connection ->
                                    connection.begin()
                                              .compose(transaction ->
                                                               Future.all(connection.preparedQuery(DatabaseAction.INSERT_ALBUM.query())
                                                                                    .executeBatch(albumQueryInput),
                                                                          connection.preparedQuery(DatabaseAction.INSERT_SONG.query())
                                                                                    .executeBatch(songQueryInput),
                                                                          connection.preparedQuery(DatabaseAction.INSERT_GENRE.query())
                                                                                    .executeBatch(genreQueryInput),
                                                                          connection.preparedQuery(DatabaseAction.INSERT_ARTIST.query())
                                                                                    .executeBatch(artistsQueryInput),
                                                                          connection.preparedQuery(DatabaseAction.INSERT_ALBUM_ARTIST.query())
                                                                                    .executeBatch(albumArtistsQueryInput))
                                                                     .compose(__ -> transaction.commit()))
                                              .eventually((Supplier<Future<Void>>) connection::close));
    }

    @Override
    public Future<List<JsonObject>> songsFromPath(final List<String> paths) {
        final String payload = paths.stream().map(o -> "\"" + o + "\"").collect(Collectors.joining(","));
        final String query = DatabaseAction.GET_SONGS_DATA_FROM_PATHS.query().replace("?", payload);
        return pool.query(query)
                   .execute()
                   .map(rows -> {
                       final List<JsonObject> songs = new ArrayList<>();
                       for (Row row : rows) {
                           songs.add(row.toJson());
                       }
                       return songs;
                   });
    }

    @Override
    public Future<JsonObject> stats() {
        return pool.query(DatabaseAction.GET_STATS.query())
                   .execute()
                   .map(rows -> rows.iterator().next().toJson());
    }

    @Override
    public Future<JsonObject> search(final String keyword) {
        final String querySearchString = "%" + keyword + "%";
        return Future.all(searchAlbums(querySearchString),
                          searchArtists(querySearchString),
                          searchSongs(querySearchString))
                     .map(compositeFuture -> {
                         final JsonObject response = new JsonObject();
                         compositeFuture.<JsonObject>list().forEach(response::mergeIn);
                         return response;
                     });
    }

    @Override
    public Future<JsonObject> song(final String path) {
        return songsFromPath(List.of(path)).map(songs -> {
            if (songs.isEmpty()) {
                return JsonObject.of();
            }
            return songs.getFirst();
        });
    }

    @Override
    public Future<List<String>> songPaths() {
        return pool.query("SELECT songs.path from songs")
                   .execute()
                   .map(rows -> {
                       final List<String> songPaths = new ArrayList<>(rows.size());
                       for (Row row : rows) {
                           songPaths.add(row.getString("path"));
                       }
                       return songPaths;
                   });
    }

    @Override
    public Future<Void> removeSongs(final List<String> paths) {
        final String payload = paths.stream().map(o -> "\"" + o + "\"").collect(Collectors.joining(","));
        final String query = DatabaseAction.DELETE_SONGS_WITH_PATHS.query().replace("?", payload);
        return pool.query(query)
                   .execute()
                   .flatMap(__ -> pool.query(DatabaseAction.CLEANUP_ALBUMS.query()).execute())
                   .flatMap(__ -> Future.succeededFuture());
    }

    @Override
    public Future<Void> clearDatabase() {
        return pool.getConnection()
                   .compose(connection -> connection.begin()
                                                    .compose(transaction -> Future.all(
                                                            connection.query(DatabaseAction.CLEAR_ALBUM_ARTISTS.query()).execute(),
                                                            connection.query(DatabaseAction.CLEAR_ARTISTS.query()).execute(),
                                                            connection.query(DatabaseAction.CLEAR_GENRES.query()).execute(),
                                                            connection.query(DatabaseAction.CLEAR_SONGS.query()).execute(),
                                                            connection.query(DatabaseAction.CLEAR_ALBUMS.query()).execute()
                                                    ).compose(__ -> transaction.commit()))
                                                    .eventually((Supplier<Future<Void>>) connection::close));
    }

    private Future<JsonObject> searchAlbums(final String keyword) {
        return pool.preparedQuery(DatabaseAction.SEARCH_ALBUMS.query())
                   .execute(Tuple.of(keyword))
                   .map(rows -> {
                       final List<JsonObject> albums = new ArrayList<>();
                       for (Row row : rows) {
                           albums.add(row.toJson());
                       }
                       return JsonObject.of("albums", albums);
                   });
    }

    private Future<JsonObject> searchSongs(final String keyword) {
        return pool.preparedQuery(DatabaseAction.SEARCH_SONGS.query())
                   .execute(Tuple.of(keyword, keyword))
                   .map(rows -> {
                       final List<JsonObject> songs = new ArrayList<>();
                       for (Row row : rows) {
                           songs.add(row.toJson());
                       }
                       return JsonObject.of("songs", songs);
                   });
    }

    private Future<JsonObject> searchArtists(final String keyword) {
        return pool.preparedQuery(DatabaseAction.SEARCH_ARTISTS.query())
                   .execute(Tuple.of(keyword))
                   .map(rows -> {
                       final List<JsonObject> artists = new ArrayList<>();
                       for (Row row : rows) {
                           artists.add(row.toJson());
                       }
                       return JsonObject.of("artists", artists);
                   });
    }
}
