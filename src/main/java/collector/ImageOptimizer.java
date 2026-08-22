package collector;

import helper.BundledLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ImageOptimizer {
    private static final Logger LOGGER = LogManager.getLogger(ImageOptimizer.class);

    private static final float WEBP_QUALITY = 75f;

    private ImageOptimizer() {
    }

    private static final class Codec {
        private static final ImageCodec CODEC = load();

        private static ImageCodec load() {
            try {
                return new ImageCodec(BundledLibrary.extract("/libimage"));
            } catch (final IOException e) {
                throw new IllegalStateException("Fail to load bundled libimage.", e);
            }
        }
    }

    public static boolean optimize(final byte[] imageBuffers, final String coverPath, final int sourceDimension, final List<Integer> variantDimensions) {
        final int[] dimensions = new int[1 + variantDimensions.size()];
        dimensions[0] = sourceDimension;
        for (int i = 0; i < variantDimensions.size(); i++) {
            dimensions[i + 1] = variantDimensions.get(i);
        }

        try {
            final byte[][] outputs = Codec.CODEC.optimize(imageBuffers, dimensions, WEBP_QUALITY);

            // normalized source image
            Files.write(Path.of(coverPath + ".webp"), outputs[0]);

            // sub variants
            for (int i = 1; i < dimensions.length; i++) {
                Files.write(Path.of(String.format("%s_%2$dx%2$d.webp", coverPath, dimensions[i])), outputs[i]);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Fail to optimize image {}", coverPath, e);
        }
        return false;
    }
}
