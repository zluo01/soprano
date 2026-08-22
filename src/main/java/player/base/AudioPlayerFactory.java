package player.base;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import player.mpv.MPVAudioPlayer;
import player.mpv.MPVInstance;

public final class AudioPlayerFactory {
    private AudioPlayerFactory() {
    }

    public static AudioPlayer create(final Vertx vertx, final JsonObject config) {
        if (PlayerType.valueOf(config.getString("type", PlayerType.MPV.name())) == PlayerType.MPV) {
            final MPVInstance instance = MPVInstance.create(config);
            // single worker thread execute to sideload mpv native call from eventloop thread
            final WorkerExecutor executor = vertx.createSharedWorkerExecutor("Player", 1);
            return new MPVAudioPlayer(instance, executor);
        }
        throw new IllegalArgumentException("Unknown player type");
    }
}
