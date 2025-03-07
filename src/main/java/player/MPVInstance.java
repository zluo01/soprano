package player;

import com.sun.jna.Native;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static config.ServerConfig.AUDIO_HARDWARE;
import static config.ServerConfig.AUDIO_OPTIONS_OVERRIDE;
import static config.ServerConfig.LIB_MPV_SOURCE_OVERRIDE;

public record MPVInstance(MPV instance, long handle) {
    private static final Logger LOGGER = LogManager.getLogger(MPVInstance.class);

    private static final Map<String, String> DEFAULT_MPV_OPTIONS = Map.of(
            "vid", "no",
            "replaygain", "no",
            "audio-display", "no",
            "audio-file-auto", "no",
            "loop-playlist", "inf",
            "loop", "no"
    );

    public static MPVInstance create(final JsonObject config) {
        final MPV instance = createInstance(config);

        final long handle = initializeMPV(instance, config);

        return new MPVInstance(instance, handle);
    }

    private static long initializeMPV(final MPV instance, final JsonObject config) {
        int error;

        final long handle = instance.mpv_create();

        if (handle == 0) {
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
                throw new IllegalStateException("Failed to get" + entry.getKey() + "with error: " + error);
            }
            LOGGER.info("MPV setting: {} => {}", entry.getKey(), option.getString(0));
        }

        return handle;
    }

    private static MPV createInstance(final JsonObject config) {
        if (config.containsKey(LIB_MPV_SOURCE_OVERRIDE)) {
            return Native.load(config.getString(LIB_MPV_SOURCE_OVERRIDE), MPV.class);
        }

        final var paths = determinePossibleMpvPath();
        for (var path : paths) {
            if (!Files.exists(Path.of(path))) {
                continue;
            }
            return Native.load(path, MPV.class);
        }

        throw new IllegalStateException("Could not find libmpv.");
    }

    private static List<String> determinePossibleMpvPath() {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            return List.of("/usr/lib64/libmpv.so", // fedora
                           "/usr/local/lib/libmpv.so", // build/install from source
                           "/usr/lib/aarch64-linux-gnu/libmpv.so" // raspbian
            );
        }

        if (osName.contains("mac")) {
            return List.of(
                    "/usr/local/lib/libmpv.dylib", // intel machine
                    "opt/homebrew/lib/libmpv.dylib" // m-chip
            );
        }

        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }
}
