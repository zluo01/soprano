package models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Objects;

@Data
@Builder
@DataObject
@AllArgsConstructor
@Accessors(fluent = true)
public final class AlbumData {
    private String name;
    private String date;
    private String artist;
    private List<SongData> songs;
    private long atime;
    private long mtime;
    private int totalDuration;

    public AlbumData(final JsonObject object) {
        this.name = object.getString("name");
        this.date = object.getString("date");
        this.artist = object.getString("artist");
        this.songs = object.getJsonArray("songs")
                           .stream()
                           .map(o -> new SongData((JsonObject) o))
                           .toList();
        this.atime = object.getLong("addTime");
        this.mtime = object.getLong("modifiedTime");
        this.totalDuration = object.getInteger("totalDuration");
    }

    public JsonObject toJson() {
        return new JsonObject().put("name", name)
                               .put("date", date)
                               .put("artist", artist)
                               .put("songs", songs.stream().map(SongData::toJson).toList())
                               .put("addTime", atime)
                               .put("modifiedTime", mtime)
                               .put("totalDuration", totalDuration);
    }

    public void incrementTotalDuration(final int duration) {
        totalDuration += duration;
    }

    public void addSong(final SongData song) {
        songs.add(song);
    }

    public void updateAddTime(final long atime) {
        if (this.atime > atime) {
            this.atime = atime;
        }
    }

    public void updateModifiedTime(final long mtime) {
        if (this.mtime > mtime) {
            this.mtime = mtime;
        }
    }

    public int generateKey() {
        return Objects.hash(name, artist);
    }
}
