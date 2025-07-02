package models;

import java.util.List;
import java.util.Objects;

public final class AlbumData {
    private final String name;
    private final String date;
    private final String artist;
    private final List<SongData> songs;
    private long atime;
    private long mtime;
    private int totalDuration;

    public AlbumData(final String name,
                     final String date,
                     final String artist,
                     final List<SongData> songs,
                     final long atime,
                     final long mtime,
                     final int totalDuration) {
        this.name = name;
        this.date = date;
        this.artist = artist;
        this.songs = songs;
        this.atime = atime;
        this.mtime = mtime;
        this.totalDuration = totalDuration;
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

    public String name() {
        return name;
    }

    public String date() {
        return date;
    }

    public String artist() {
        return artist;
    }

    public List<SongData> songs() {
        return songs;
    }

    public long atime() {
        return atime;
    }

    public long mtime() {
        return mtime;
    }

    public int totalDuration() {
        return totalDuration;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String date;
        private String artist;
        private List<SongData> songs;
        private long atime;
        private long mtime;
        private int totalDuration;

        private Builder() {
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

        public Builder songs(final List<SongData> songs) {
            this.songs = songs;
            return this;
        }

        public Builder atime(final long atime) {
            this.atime = atime;
            return this;
        }

        public Builder mtime(final long mtime) {
            this.mtime = mtime;
            return this;
        }

        public Builder totalDuration(final int totalDuration) {
            this.totalDuration = totalDuration;
            return this;
        }

        public AlbumData build() {
            return new AlbumData(name, date, artist, songs, atime, mtime, totalDuration);
        }
    }
}
