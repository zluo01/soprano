package models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@DataObject
@AllArgsConstructor
public final class Song {
    private String name;
    private String artists;
    private int albumId;
    private String album;
    private String path;
    private int disc;
    private int trackNum;
    private int duration;

    public Song(final JsonObject song) {
        this.name = song.getString("name");
        this.artists = song.getString("artists");
        this.albumId = song.getInteger("albumId");
        this.album = song.getString("album");
        this.path = song.getString("path");
        this.disc = song.getInteger("disc");
        this.trackNum = song.getInteger("trackNum");
        this.duration = song.getInteger("duration");
    }

    public JsonObject toJson() {
        return new JsonObject().put("name", name)
                               .put("artists", artists)
                               .put("albumId", albumId)
                               .put("album", album)
                               .put("path", path)
                               .put("disc", disc)
                               .put("trackNum", trackNum)
                               .put("duration", duration);
    }
}
