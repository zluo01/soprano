package collector;

import helper.BundledLibrary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ImageOptimizer {
    private static final Logger LOGGER = LogManager.getLogger(ImageOptimizer.class);

    private static final float WEBP_QUALITY = 75f;

    private ImageOptimizer() {
    }

    private static final class Encoder {
        private static final WebP WEBP = load();

        private static WebP load() {
            try {
                return new WebP(BundledLibrary.extract("/libwebp"));
            } catch (final IOException e) {
                throw new IllegalStateException("Fail to load bundled libwebp.", e);
            }
        }
    }

    public static boolean optimize(final byte[] imageBuffers, final String coverPath, final int sourceDimension, final List<Integer> variantDimensions) {
        try (InputStream stream = new ByteArrayInputStream(imageBuffers)) {
            final BufferedImage source = ImageIO.read(stream);
            if (source == null) {
                LOGGER.error("Unsupported image format: {}", coverPath);
                return false;
            }

            // normalize source image
            writeWebp(source, sourceDimension, coverPath + ".webp");

            // create sub variant
            for (final int dimension : variantDimensions) {
                writeWebp(source, dimension, String.format("%s_%2$dx%2$d.webp", coverPath, dimension));
            }
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Fail to optimize image {}", coverPath, e);
        }
        return false;
    }

    private static void writeWebp(final BufferedImage source, final int size, final String outputPath) throws IOException {
        final BufferedImage canvas = scaleAndPad(source, size);
        final byte[] webp = Encoder.WEBP.encodeRGBA(rgbaBytes(canvas), size, size, WEBP_QUALITY);
        Files.write(Path.of(outputPath), webp);
    }

    /**
     * Aspect-fit the image into a size x size square, centered on a
     * transparent background.
     */
    private static BufferedImage scaleAndPad(final BufferedImage source, final int size) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        final int targetWidth;
        final int targetHeight;
        if (width > height) {
            targetWidth = size;
            targetHeight = Math.max(1, Math.round(size * (float) height / width));
        } else {
            targetHeight = size;
            targetWidth = Math.max(1, Math.round(size * (float) width / height));
        }

        final BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, (size - targetWidth) / 2, (size - targetHeight) / 2, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return canvas;
    }

    private static byte[] rgbaBytes(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        final byte[] rgba = new byte[argb.length * 4];
        for (int i = 0; i < argb.length; i++) {
            final int pixel = argb[i];
            rgba[i * 4] = (byte) (pixel >> 16);
            rgba[i * 4 + 1] = (byte) (pixel >> 8);
            rgba[i * 4 + 2] = (byte) pixel;
            rgba[i * 4 + 3] = (byte) (pixel >>> 24);
        }
        return rgba;
    }
}
