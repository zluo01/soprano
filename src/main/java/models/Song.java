package models;

import io.vertx.core.json.JsonObject;

import java.util.Objects;

public record Song(String name,
                   String artists,
                   int albumId,
                   String album,
                   String path,
                   int disc,
                   int trackNum,
                   int duration
) {
    public Song {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(artists, "artists is required");
        Objects.requireNonNull(album, "album is required");
        Objects.requireNonNull(path, "path is required");
    }

    public Song(final JsonObject song) {
        this(song.getString("name"),
             song.getString("artists"),
             song.getInteger("albumId"),
             song.getString("album"),
             song.getString("path"),
             song.getInteger("disc"),
             song.getInteger("trackNum"),
             song.getInteger("duration"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String artists;
        private int albumId;
        private String album;
        private String path;
        private int disc;
        private int trackNum;
        private int duration;

        private Builder() {
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder artists(final String artists) {
            this.artists = artists;
            return this;
        }

        public Builder albumId(final int albumId) {
            this.albumId = albumId;
            return this;
        }

        public Builder album(final String album) {
            this.album = album;
            return this;
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public Builder disc(final int disc) {
            this.disc = disc;
            return this;
        }

        public Builder trackNum(final int trackNum) {
            this.trackNum = trackNum;
            return this;
        }

        public Builder duration(final int duration) {
            this.duration = duration;
            return this;
        }

        public Song build() {
            return new Song(name, artists, albumId, album, path, disc, trackNum, duration);
        }
    }
}
