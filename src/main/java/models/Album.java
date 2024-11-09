package models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@DataObject
@AllArgsConstructor
@Accessors(fluent = true)
public final class Album {
    private int id;
    private String name;
    private String date;
    private String artist;
    private List<Song> songs;
    private long addTime;
    private long modifiedTime;
    private int totalDuration;

    public Album(final JsonObject object) {
        this.id = object.getInteger("id");
        this.name = object.getString("name");
        this.date = object.getString("date");
        this.artist = object.getString("artist");
        this.songs = object.getJsonArray("songs")
                           .stream()
                           .map(o -> new Song((JsonObject) o))
                           .toList();
        this.addTime = object.getLong("addTime");
        this.modifiedTime = object.getLong("modifiedTime");
        this.totalDuration = object.getInteger("totalDuration");
    }

    public JsonObject toJson() {
        return new JsonObject().put("id", id)
                               .put("name", name)
                               .put("date", date)
                               .put("artist", artist)
                               .put("songs", songs.stream().map(Song::toJson).toList())
                               .put("addTime", addTime)
                               .put("modifiedTime", modifiedTime)
                               .put("totalDuration", totalDuration);
    }
}
