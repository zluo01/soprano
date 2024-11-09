package config;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerConfig {
    public static final String DATABASE_CONFIG = "DATABASE_CONFIG";

    private static final String BASE_CONFIG_PATH = System.getProperty("user.home") + "/.config/mesa/";

    public static final String CONFIG_FILE_PATH = BASE_CONFIG_PATH + "mesa.properties";

    public static final String COVER_PATH = BASE_CONFIG_PATH + "cover";

    public static final String PLAYLIST_PATH = BASE_CONFIG_PATH + "playlists";

    // configuration keys
    public static final String MUSIC_DIRECTORY_CONFIG = "directory.music";

    public static final String SERVER_PORT_CONFIG = "server.port";

    public static final String DATABASE_FILE_PATH = BASE_CONFIG_PATH + "main.sqlite";

    public static final String COVER_SOURCE_DIMENSION = "cover.source";

    public static final String COVER_VARIANT_DIMENSION = "cover.variant";

    private static final int DEFAULT_PORT = 6868;

    private static final int DEFAULT_COVER_SOURCE_DIMENSION = 800;

    private ServerConfig() {
    }

    public static Future<JsonObject> verifyConfig(final JsonObject config) {
        if (config.containsKey(COVER_VARIANT_DIMENSION) && !isValidVariantList(config.getString(COVER_VARIANT_DIMENSION))) {
            return Future.failedFuture("Cover variant should be a comma separated list of integer, but get "
                                       + config.getString(COVER_VARIANT_DIMENSION));
        }
        return Future.succeededFuture(config);
    }

    private static boolean isValidVariantList(final String input) {
        // Regular expression to match integers separated by commas
        String regex = "^\\[(-?\\d+)(,-?\\d+)*]$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static int serverPort(final JsonObject config) {
        return config.getInteger(SERVER_PORT_CONFIG, DEFAULT_PORT);
    }

    public static int coverSourceDimension(final JsonObject config) {
        return config.getInteger(COVER_SOURCE_DIMENSION, DEFAULT_COVER_SOURCE_DIMENSION);
    }

    public static List<Integer> coverVariants(final JsonObject config) {
        if (config.containsKey(COVER_VARIANT_DIMENSION)) {
            return config.getJsonArray(COVER_VARIANT_DIMENSION)
                         .stream()
                         .map(o -> (Integer) o)
                         .toList();
        }
        return List.of();
    }
}
