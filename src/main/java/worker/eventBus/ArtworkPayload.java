package worker.eventBus;

import config.ServerConfig;
import org.jaudiotagger.tag.images.Artwork;

import java.nio.file.Path;

public record ArtworkPayload(String destination, byte[] imageData) {
    public static ArtworkPayload of(final int albumId, final Artwork artwork) {
        final Path coverFilePath = Path.of(ServerConfig.COVER_PATH).resolve(albumId + ".png");
        return new ArtworkPayload(coverFilePath.toString(), artwork.getBinaryData());
    }
}
