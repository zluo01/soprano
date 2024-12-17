package collector;

import models.SongData;
import org.jaudiotagger.tag.images.Artwork;

public record SongPayload(SongData song, Artwork artwork) {
}
