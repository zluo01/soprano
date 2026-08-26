package helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

public final class BundledLibrary {

    private BundledLibrary() {
    }

    /**
     * Extract gzip-compressed native bundle library to a temporary file for loading
     *
     * @param resource path under the resources folder, must end with .gz
     * @return temp file path
     * @throws IOException exception
     */
    public static Path extract(final String resource) throws IOException {
        try (InputStream in = BundledLibrary.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing bundled library resource " + resource);
            }
            final String name = resource.substring(0, resource.length() - ".gz".length());
            // .bin suffix prevents Windows from appending .dll to the binary
            final Path lib = Files.createTempFile("soprano" + name.replace('/', '-') + "-", ".bin");
            lib.toFile().deleteOnExit();
            try (InputStream decompressed = new GZIPInputStream(in)) {
                Files.copy(decompressed, lib, StandardCopyOption.REPLACE_EXISTING);
            }
            return lib;
        }
    }
}
