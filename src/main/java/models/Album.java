package models;

import java.util.List;
import java.util.Objects;

public record Album(int id,
                    String name,
                    String date,
                    String artist,
                    List<Song> songs,
                    long addTime,
                    long modifiedTime,
                    int totalDuration
) {
    public Album {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(date, "date is required");
        Objects.requireNonNull(artist, "artist is required");
        Objects.requireNonNull(songs, "songs is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int id;
        private String name;
        private String date;
        private String artist;
        private List<Song> songs;
        private long addTime;
        private long modifiedTime;
        private int totalDuration;

        private Builder() {
        }

        public Builder id(final int id) {
            this.id = id;
            return this;
        }

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder date(final String date) {
            this.date = date;
            return this;
        }

        public Builder artist(final String artist) {
            this.artist = artist;
            return this;
        }

        public Builder songs(final List<Song> songs) {
            this.songs = songs;
            return this;
        }

        public Builder addTime(final long addTime) {
            this.addTime = addTime;
            return this;
        }

        public Builder modifiedTime(final long modifiedTime) {
            this.modifiedTime = modifiedTime;
            return this;
        }

        public Builder totalDuration(final int totalDuration) {
            this.totalDuration = totalDuration;
            return this;
        }

        public Album build() {
            return new Album(id, name, date, artist, songs, addTime, modifiedTime, totalDuration);
        }
    }
}
