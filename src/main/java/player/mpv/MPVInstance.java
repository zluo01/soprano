package player.mpv;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import static config.ServerConfig.AUDIO_HARDWARE;
import static config.ServerConfig.AUDIO_OPTIONS_OVERRIDE;

public record MPVInstance(MPV instance, MemorySegment handle) {
    private static final Logger LOGGER = LogManager.getLogger(MPVInstance.class);

    private static final Map<String, String> DEFAULT_MPV_OPTIONS = Map.of(
            "vid", "no",
            "replaygain", "no",
            "audio-display", "no",
            "audio-file-auto", "no",
            "loop-playlist", "no",
            "loop", "no"
    );

    public static MPVInstance create(final JsonObject config) {
        final MPV instance = loadLibMpv();

        final MemorySegment handle = initializeMPV(instance, config);

        return new MPVInstance(instance, handle);
    }

    private static MemorySegment initializeMPV(final MPV instance, final JsonObject config) {
        int error;

        final MemorySegment handle = instance.mpv_create();

        if (handle.address() == 0) {
            throw new IllegalStateException("Failed to create MPV instance");
        }

        final Map<String, String> mpvOptions = new HashMap<>(DEFAULT_MPV_OPTIONS);

        if (config.containsKey(AUDIO_HARDWARE)) {
            mpvOptions.put("audio-device", config.getString(AUDIO_HARDWARE));
        }

        if (config.containsKey(AUDIO_OPTIONS_OVERRIDE)) {
            final var options = config.getString(AUDIO_OPTIONS_OVERRIDE).split(",");
            for (final String option : options) {
                if (option.trim().isEmpty()) {
                    continue;
                }
                var optionValue = option.split("=", 2);
                mpvOptions.put(optionValue[0], optionValue[1]);
            }
        }

        for (Map.Entry<String, String> entry : mpvOptions.entrySet()) {
            error = instance.mpv_set_option_string(handle, entry.getKey(), entry.getValue());
            if (error != 0) {
                throw new IllegalStateException("Failed to set " + entry.getKey() + " with error: " + error);
            }
        }

        error = instance.mpv_initialize(handle);
        if (error != 0) {
            throw new IllegalStateException("Failed to initialize options: " + error);
        }

        for (Map.Entry<String, String> entry : mpvOptions.entrySet()) {
            final var option = instance.mpv_get_property_string(handle, entry.getKey());
            if (option == null) {
                throw new IllegalStateException("Failed to get " + entry.getKey());
            }
            LOGGER.info("MPV setting: {} => {}", entry.getKey(), option);
        }

        return handle;
    }

    private static MPV loadLibMpv() {
        try {
            final Path lib = extractBundledLibrary();
            LOGGER.info("Loading bundled libmpv from {}", lib);
            return new MPV(lib);
        } catch (final IOException e) {
            throw new IllegalStateException("Fail to load bundle libmpv.", e);
        }
    }

    /**
     * Extract the build-in libmpv binary to a file under tmp folder for loading
     */
    private static Path extractBundledLibrary() throws IOException {
        try (InputStream in = MPVInstance.class.getResourceAsStream("/libmpv")) {
            if (in == null) {
                throw new IOException("Missing bundled library resource /libmpv");
            }
            final Path lib = Files.createTempFile("soprano-libmpv-", ".bin");
            lib.toFile().deleteOnExit();
            Files.copy(in, lib, StandardCopyOption.REPLACE_EXISTING);
            return lib;
        }
    }
}
