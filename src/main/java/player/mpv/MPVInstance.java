package player.mpv;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static config.ServerConfig.AUDIO_HARDWARE;
import static config.ServerConfig.AUDIO_OPTIONS_OVERRIDE;

public record MPVInstance(MPV instance, long handle) {
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
            try {
                LOGGER.info("MPV setting: {} => {}", entry.getKey(), option.getString(0));
            } finally {
                instance.mpv_free(option);
            }
        }

        return handle;
    }

    private static MPV loadLibMpv() {
        try {
            final File lib = Native.extractFromResourcePath("mpv", MPVInstance.class.getClassLoader());
            LOGGER.info("Loading bundled libmpv from {}", lib.getAbsolutePath());
            return Native.load(lib.getAbsolutePath(), MPV.class);
        } catch (final IOException e) {
            throw new IllegalStateException("No bundled libmpv for platform " + Platform.RESOURCE_PREFIX, e);
        }
    }
}
