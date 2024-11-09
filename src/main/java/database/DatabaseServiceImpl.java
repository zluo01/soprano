package database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
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


public class DatabaseServiceImpl implements DatabaseService {

    private final JDBCPool pool;

    public DatabaseServiceImpl(final JDBCPool pool) {
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
                       Album.AlbumBuilder albumBuilder = null;
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
    public Future<Void> scan(final List<AlbumData> albums) {
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
                   .compose(connection -> connection.begin()
                                                    .compose(transaction -> connection
                                                            // clear old data
                                                            .query(DatabaseAction.CLEAR_ALBUM_ARTISTS.query()).execute()
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CLEAR_ARTISTS.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CLEAR_GENRES.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CLEAR_SONGS.query())
                                                                    .execute())
                                                            .compose(__ -> connection
                                                                    .query(DatabaseAction.CLEAR_ALBUMS.query())
                                                                    .execute())
                                                            // new data
                                                            .compose(__ -> connection
                                                                    .preparedQuery(DatabaseAction.INSERT_ALBUM.query())
                                                                    .executeBatch(albumQueryInput))
                                                            .compose(__ -> connection
                                                                    .preparedQuery(DatabaseAction.INSERT_SONG.query())
                                                                    .executeBatch(songQueryInput))
                                                            .compose(__ -> connection
                                                                    .preparedQuery(DatabaseAction.INSERT_GENRE.query())
                                                                    .executeBatch(genreQueryInput))
                                                            .compose(__ -> connection
                                                                    .preparedQuery(DatabaseAction.INSERT_ARTIST.query())
                                                                    .executeBatch(artistsQueryInput))
                                                            .compose(__ -> connection
                                                                    .preparedQuery(DatabaseAction.INSERT_ALBUM_ARTIST.query())
                                                                    .executeBatch(albumArtistsQueryInput))
                                                            .compose(__ -> transaction.commit()))
                                                    .eventually((Supplier<Future<Void>>) connection::close));
    }
}
