package collector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ImageOptimizer {
    private static final Logger LOGGER = LogManager.getLogger(ImageOptimizer.class);

    private ImageOptimizer() {
    }

    public static boolean optimize(final String filePath, final int sourceDimension, final List<Integer> variantDimensions) {
        final String[] sourceCommand = sourceCommands(filePath, sourceDimension);
        try {
            // normalize source image
            if (0 != runCommand(sourceCommand)) {
                return false;
            }

            // create sub variant
            for (int dimension : variantDimensions) {
                final String[] variantCommends = commands(filePath, dimension);
                if (0 != runCommand(variantCommends)) {
                    return false;
                }
            }
            return true;
        } catch (IOException | InterruptedException e) {
            LOGGER.error(e);
        }
        return false;
    }

    private static String[] sourceCommands(final String filePath, final int size) {
        final String format = String.format("scale='if(gt(a,1),%1$d,-1)':'if(gt(a,1),-1,%1$d)',pad=%1$d:%1$d:(%1$d-iw)/2:(%1$d-ih)/2:color=0x00000000", size);
        return new String[]{
                "ffmpeg",
                "-y",
                "-i", filePath,
                "-vf", format,
                "-compression_level", "6",
                "-loglevel", "fatal",
                filePath.replace(".png", ".webp")
        };
    }

    private static String[] commands(final String filePath, final int size) {
        final String format = String.format("scale='if(gt(a,1),%1$d,-1)':'if(gt(a,1),-1,%1$d)',pad=%1$d:%1$d:(%1$d-iw)/2:(%1$d-ih)/2:color=0x00000000", size);
        return new String[]{
                "ffmpeg",
                "-y",
                "-i", filePath,
                "-vf", format,
                "-q:v", "75",
                "-compression_level", "6",
                "-loglevel", "fatal",
                filePath.replace(".png", String.format("_%1$dx%1$d.webp", size))
        };
    }

    private static int runCommand(final String[] command) throws InterruptedException, IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();

        // Read the output
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            LOGGER.error(line);
        }

        // Wait for the process to complete
        return process.waitFor();
    }
}
