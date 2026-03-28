package collector;

import models.AlbumData;
import models.SongData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlbumDataAggregationTest {

    private SongData song(String title, int duration, long atime, long mtime) {
        return SongData.builder()
                       .path("/music/" + title + ".flac")
                       .artist("Artist")
                       .album("Album")
                       .albumArtist("Artist")
                       .title(title)
                       .genre("Rock")
                       .date("2024")
                       .composer("")
                       .performer("")
                       .disc(1)
                       .trackNum(1)
                       .duration(duration)
                       .atime(atime)
                       .mtime(mtime)
                       .build();
    }

    private SongData song(String title, int duration) {
        return song(title, duration, 1000L, 2000L);
    }

    @Test
    void totalDurationSumsAllSongs() {
        final List<SongData> songs = List.of(song("A", 100), song("B", 200), song("C", 150));

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(songs);

        final AlbumData album = result.values().iterator().next();
        assertEquals(3, album.songs().size());
        assertEquals(450, album.totalDuration());
    }

    @Test
    void singleSongDurationNotDoubled() {
        final List<SongData> songs = List.of(song("Only", 300));

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(songs);

        final AlbumData album = result.values().iterator().next();
        assertEquals(1, album.songs().size());
        assertEquals(300, album.totalDuration());
    }

    @Test
    void multipleAlbumsAggregatedSeparately() {
        final SongData song1 = SongData.builder()
                .path("/music/a.flac").artist("A").album("Album1").albumArtist("A")
                .title("Song1").genre("").date("2024").composer("").performer("")
                .disc(1).trackNum(1).duration(100).atime(1000L).mtime(2000L).build();
        final SongData song2 = SongData.builder()
                .path("/music/b.flac").artist("B").album("Album2").albumArtist("B")
                .title("Song2").genre("").date("2024").composer("").performer("")
                .disc(1).trackNum(1).duration(200).atime(1000L).mtime(2000L).build();

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(List.of(song1, song2));

        assertEquals(2, result.size());
        for (AlbumData album : result.values()) {
            assertEquals(1, album.songs().size());
            assertEquals(album.songs().getFirst().duration(), album.totalDuration());
        }
    }

    @Test
    void addTimeKeepsMinimum() {
        final List<SongData> songs = List.of(
                song("A", 100, 3000L, 1000L),
                song("B", 100, 1000L, 1000L),
                song("C", 100, 2000L, 1000L));

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(songs);

        assertEquals(1000L, result.values().iterator().next().atime());
    }

    @Test
    void modifiedTimeKeepsMinimum() {
        final List<SongData> songs = List.of(
                song("A", 100, 1000L, 3000L),
                song("B", 100, 1000L, 1000L),
                song("C", 100, 1000L, 2000L));

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(songs);

        assertEquals(1000L, result.values().iterator().next().mtime());
    }

    @Test
    void albumArtistFallsBackToSongArtist() {
        final SongData song = SongData.builder()
                .path("/music/a.flac").artist("SongArtist").album("Album").albumArtist("")
                .title("Song").genre("").date("2024").composer("").performer("")
                .disc(1).trackNum(1).duration(100).atime(1000L).mtime(2000L).build();

        final Map<Integer, AlbumData> result = AudioDataCollectorVerticle.aggregateSongData(List.of(song));

        assertEquals("SongArtist", result.values().iterator().next().artist());
    }
}
