package player;

import com.sun.jna.Native;
import io.vertx.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static config.ServerConfig.AUDIO_HARDWARE;
import static config.ServerConfig.LIB_MPV_SOURCE_OVERRIDE;

public record MPVInstance(MPV instance, long handle) {

    private static final Map<String, String> DEFAULT_MPV_OPTIONS = Map.of(
            "vo", "null",
            "replaygain", "no",
            "audio-display", "no",
            "audio-file-auto", "no"
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

        for (Map.Entry<String, String> entry : mpvOptions.entrySet()) {
            error = instance.mpv_set_option_string(handle, entry.getKey(), entry.getValue());
            if (error != 0) {
                throw new IllegalStateException("Failed to set" + entry.getKey() + "with error: " + error);
            }
        }

        error = instance.mpv_initialize(handle);
        if (error != 0) {
            throw new IllegalStateException("Failed to initialize options: " + error);
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
