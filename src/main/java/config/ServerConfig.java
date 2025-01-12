package config;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerConfig {
    private static final int DEFAULT_COVER_SOURCE_DIMENSION = 800;

    private static final Level DEAULT_LOG_LEVEL = Level.ERROR;

    private static final List<Integer> DEFAULT_COVER_VARIANTS = List.of(50, 180);


    public static final String DATABASE_CONFIG = "DATABASE_CONFIG";

    private static final Path BASE_CONFIG_PATH = getConfigDataPath();

    public static final String CONFIG_FILE_PATH = BASE_CONFIG_PATH.resolve("soprano.properties").toString();

    public static final String COVER_PATH = BASE_CONFIG_PATH.resolve("cover").toString();

    public static final String PLAYLIST_PATH = BASE_CONFIG_PATH.resolve("playlists").toString();

    public static final String DATABASE_FILE_PATH = BASE_CONFIG_PATH.resolve("main.sqlite").toString();

    // configuration keys
    public static final String MUSIC_DIRECTORY_CONFIG = "directory.music";

    public static final String DEBUG_SWITCH_CONFIG = "debug.switch";

    public static final String LOG_LEVEL = "log.level";

    public static final String WEB_UI_ENABLE = "webUI.enable";

    public static final String COVER_SOURCE_DIMENSION = "cover.source";

    public static final String COVER_VARIANT_DIMENSION = "cover.variant";

    public static final String LIB_MPV_SOURCE_OVERRIDE = "lib.mpv.source.override";

    public static final String AUDIO_HARDWARE = "audio.hardware";

    public static final String AUDIO_OPTIONS_OVERRIDE = "audio.options.override";

    private ServerConfig() {
    }

    public static Future<JsonObject> verifyAndSetupConfig(final JsonObject config) {
        if (!config.containsKey(MUSIC_DIRECTORY_CONFIG)
            || config.getString(MUSIC_DIRECTORY_CONFIG) == null
            || config.getString(MUSIC_DIRECTORY_CONFIG).isEmpty()
            || !Files.exists(Path.of(config.getString(MUSIC_DIRECTORY_CONFIG)))) {
            return Future.failedFuture("Fail to find valid music directory" + config.getString(MUSIC_DIRECTORY_CONFIG));
        }

        // validate
        if (config.containsKey(COVER_VARIANT_DIMENSION) && !isValidVariantList(config.getString(COVER_VARIANT_DIMENSION))) {
            return Future.failedFuture("Cover variant should be a comma separated list of integer, but get "
                                       + config.getString(COVER_VARIANT_DIMENSION));
        }

        if (isWindows() && !config.containsKey(LIB_MPV_SOURCE_OVERRIDE)) {
            return Future.failedFuture("Require libmpv source override to be set in Windows.");
        }

        // configure
        configGlobalLogLevel(Level.toLevel(config.getString(LOG_LEVEL), DEAULT_LOG_LEVEL));
        return Future.succeededFuture(config);
    }

    private static boolean isValidVariantList(final String input) {
        // Regular expression to match integers separated by commas
        final String regex = "^\\[(-?\\d+)(,-?\\d+)*]$";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static String musicDirectory(final JsonObject config) {
        final String directory = config.getString(MUSIC_DIRECTORY_CONFIG);
        if (directory.contains("~")) {
            return directory.replace("~", System.getProperty("user.home"));
        }
        return directory;
    }

    public static boolean isWebUiEnabled(final JsonObject config) {
        return config.getBoolean(WEB_UI_ENABLE, true);
    }

    public static int coverSourceDimension(final JsonObject config) {
        return config.getInteger(COVER_SOURCE_DIMENSION, DEFAULT_COVER_SOURCE_DIMENSION);
    }

    public static boolean enableGraphQLDebug(final JsonObject config) {
        return config.getBoolean(DEBUG_SWITCH_CONFIG, false);
    }

    public static List<Integer> coverVariants(final JsonObject config) {
        final var variants = new HashSet<Integer>();
        if (config.containsKey(COVER_VARIANT_DIMENSION)) {
            config.getJsonArray(COVER_VARIANT_DIMENSION)
                  .stream()
                  .forEach(o -> variants.add((Integer) o));
        }

        if (isWebUiEnabled(config)) {
            variants.addAll(DEFAULT_COVER_VARIANTS);
        }

        return new ArrayList<>(variants);
    }

    /**
     * <a href="https://stackoverflow.com/a/23434603">Configure log level</a>
     *
     * @param level log level to set
     */
    public static void configGlobalLogLevel(final Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }

    private static Path getConfigDataPath() {
        if (isWindows()) {
            return Path.of(System.getenv("APPDATA"), "soprano");
        }
        return Path.of(System.getProperty("user.home"), ".config", "soprano");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
