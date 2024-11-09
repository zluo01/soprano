package worker.scan;

import config.ServerConfig;
import org.jaudiotagger.tag.images.Artwork;

import java.nio.file.Path;

record ArtworkPayload(String destination, byte[] imageData) {
    static ArtworkPayload of(final int key, final Artwork artwork) {
        final Path coverFilePath = Path.of(ServerConfig.COVER_PATH).resolve(key + ".png");
        return new ArtworkPayload(coverFilePath.toString(), artwork.getBinaryData());
    }
}
