package player.mpv;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import player.base.AudioPlayer;
import player.base.PlaybackStatus;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * <a href="https://mpv.io/manual/master/#playlist-manipulation">playlist commands</a>
 * <a href="https://mpv.io/manual/master/#properties">MPV properties</a>
 */
public final class MPVAudioPlayer implements AudioPlayer {
    private static final Logger LOGGER = LogManager.getLogger(MPVAudioPlayer.class);

    private final MPVInstance mpv;
    private volatile boolean running = true;
    private Thread monitorThread;

    public MPVAudioPlayer(final MPVInstance mpv) {
        this.mpv = mpv;
    }

    @Override
    public Future<Integer> play(final String songPath) {
        try {
            return Future.succeededFuture(playSongFromPath(songPath));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    @Override
    public Future<Integer> pause() {
        final var pauseProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "pause");
        if (pauseProperty == null) {
            return Future.failedFuture("Failed to get pause status.");
        }

        try {
            final var pauseStatus = pauseProperty.getString(0).equals("yes") ? "no" : "yes";
            final int error = mpv.instance().mpv_set_property_string(mpv.handle(), "pause", pauseStatus);
            if (error != 0) {
                return Future.failedFuture("Failed to pause: " + MPVError.getError(error));
            }
            return Future.succeededFuture(error);
        } finally {
            mpv.instance().mpv_free(pauseProperty);
        }
    }

    @Override
    public Future<Integer> stop() {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"stop"});
        if (error != 0) {
            return Future.failedFuture("Failed to clear queue: " + MPVError.getError(error));
        }
        return Future.succeededFuture(error);
    }

    @Override
    public Future<PlaybackStatus> playbackStatus() {
        final var pauseProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "pause");
        if (pauseProperty == null) {
            return Future.failedFuture("Failed to get pause status.");
        }

        final boolean isPlaying;
        try {
            isPlaying = pauseProperty.getString(0).equals("no");
        } finally {
            mpv.instance().mpv_free(pauseProperty);
        }

        final var playbackTimeProperty = mpv.instance().mpv_get_property_string(mpv.handle(), "playback-time");
        if (playbackTimeProperty == null) {
            return Future.failedFuture("Fail to get playback time");
        }

        try {
            final double playbackTime = Double.parseDouble(playbackTimeProperty.getString(0));
            return Future.succeededFuture(new PlaybackStatus(isPlaying, (int) playbackTime));
        } finally {
            mpv.instance().mpv_free(playbackTimeProperty);
        }
    }

    @Override
    public void startMonitor(final Supplier<Optional<String>> nextSong, final Runnable changeSong) {
        monitorThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    final MPV.mpv_event event = mpv.instance().mpv_wait_event(mpv.handle(), 1.0);

                    if (event.event_id == MPVEventId.MPV_EVENT_NONE) {
                        continue;
                    }

                    // on song change
                    if (event.event_id == MPVEventId.MPV_EVENT_FILE_LOADED) {
                        changeSong.run();
                    }

                    // on song stop
                    if (event.event_id == MPVEventId.MPV_EVENT_END_FILE) {
                        final MPV.mpv_event_end_file endFile = new MPV.mpv_event_end_file(event.data);
                        if (endFile.reason == MPVEndFileReason.MPV_END_FILE_REASON_EOF) {
                            nextSong.get().ifPresent(this::playSongFromPath);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("MPV monitor thread error", e);
                }
            }
        }, "mpv-monitor-thread");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void close() {
        running = false;
        try {
            if (monitorThread != null) {
                monitorThread.join(5000);
                if (monitorThread.isAlive()) {
                    LOGGER.warn("Monitor thread did not stop within timeout");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for monitor thread to stop");
        }
    }

    private int playSongFromPath(final String songPath) {
        final int error = mpv.instance().mpv_command(mpv.handle(), new String[]{"loadfile", songPath});
        if (error != 0) {
            throw new IllegalStateException("Failed to load file: " + MPVError.getError(error));
        }
        return error;
    }
}
