package models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@DataObject
@AllArgsConstructor
@Accessors(fluent = true)
public final class SongData {
    /**
     * absolute file path
     */
    private String path;
    private String artist;
    private String album;
    private String albumArtist;
    private String title;
    private String genre;
    private String date;
    private String composer;
    private String performer;
    private int disc;
    private int trackNum;
    private int duration;
    /**
     * last modified time
     */
    private long mtime;
    /**
     * song add time
     */
    private long atime;

    public SongData(final JsonObject song) {
        this.path = song.getString("path");
        this.artist = song.getString("artist");
        this.album = song.getString("album");
        this.albumArtist = song.getString("albumArtist");
        this.title = song.getString("title");
        this.genre = song.getString("genre");
        this.date = song.getString("date");
        this.composer = song.getString("composer");
        this.performer = song.getString("performer");
        this.disc = song.getInteger("disc");
        this.trackNum = song.getInteger("trackNum");
        this.duration = song.getInteger("duration");
        this.mtime = song.getLong("mtime");
        this.atime = song.getLong("atime");
    }

    public JsonObject toJson() {
        return new JsonObject().put("path", path)
                               .put("artist", artist)
                               .put("album", album)
                               .put("albumArtist", albumArtist)
                               .put("title", title)
                               .put("genre", genre)
                               .put("date", path)
                               .put("composer", composer)
                               .put("performer", performer)
                               .put("disc", disc)
                               .put("trackNum", trackNum)
                               .put("duration", duration)
                               .put("mtime", mtime)
                               .put("atime", atime);
    }
}
