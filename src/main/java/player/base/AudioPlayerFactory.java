package player.base;

import io.vertx.core.json.JsonObject;
import player.mpv.MPVInstance;
import player.mpv.MPVAudioPlayer;

public final class AudioPlayerFactory {
    private AudioPlayerFactory() {
    }

    public static AudioPlayer create(final JsonObject config) {
        if (PlayerType.valueOf(config.getString("type", PlayerType.MPV.name())) == PlayerType.MPV) {
            final MPVInstance instance = MPVInstance.create(config);
            return new MPVAudioPlayer(instance);
        }
        throw new IllegalArgumentException("Unknown player type");
    }
}
