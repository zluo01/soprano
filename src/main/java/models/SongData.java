package models;

import java.util.Objects;

public record SongData(String path, //absolute file path
                       String artist,
                       String album,
                       String albumArtist,
                       String title,
                       String genre,
                       String date,
                       String composer,
                       String performer,
                       int disc,
                       int trackNum,
                       int duration,
                       long mtime, //last modified time
                       long atime //song add time
) {
    public SongData {
        Objects.requireNonNull(path, "path is null");
        Objects.requireNonNull(artist, "artist is null");
        Objects.requireNonNull(album, "album is null");
        Objects.requireNonNull(albumArtist, "albumArtist is null");
        Objects.requireNonNull(title, "title is null");
        Objects.requireNonNull(genre, "genre is null");
        Objects.requireNonNull(date, "date is null");
        Objects.requireNonNull(composer, "composer is null");
        Objects.requireNonNull(performer, "performer is null");
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
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
        private long mtime;
        private long atime;

        private Builder() {
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public Builder artist(final String artist) {
            this.artist = artist;
            return this;
        }

        public Builder album(final String album) {
            this.album = album;
            return this;
        }

        public Builder albumArtist(final String albumArtist) {
            this.albumArtist = albumArtist;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder genre(final String genre) {
            this.genre = genre;
            return this;
        }

        public Builder date(final String date) {
            this.date = date;
            return this;
        }

        public Builder composer(final String composer) {
            this.composer = composer;
            return this;
        }

        public Builder performer(final String performer) {
            this.performer = performer;
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

        public Builder mtime(final long mtime) {
            this.mtime = mtime;
            return this;
        }

        public Builder atime(final long atime) {
            this.atime = atime;
            return this;
        }

        public SongData build() {
            return new SongData(path,
                                artist,
                                album,
                                albumArtist,
                                title,
                                genre,
                                date,
                                composer,
                                performer,
                                disc,
                                trackNum,
                                duration,
                                mtime,
                                atime);
        }
    }
}
