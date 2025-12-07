package player.base;

import io.vertx.core.Future;

import java.util.Optional;
import java.util.function.Supplier;

public interface AudioPlayer {
    Future<Integer> play(String songPath);

    Future<Integer> pause();

    Future<Integer> stop();

    Future<PlaybackStatus> playbackStatus();

    void startMonitor(Supplier<Optional<String>> action);

    void close();
}
