package player;

import com.google.common.annotations.VisibleForTesting;
import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.serviceproxy.ServiceBinder;
import player.base.AudioPlayer;
import player.base.AudioPlayerFactory;
import playlists.PlaylistService;

import java.util.Optional;

public class PlayerVerticle extends VerticleBase {

    private final DatabaseService databaseService;
    private final PlaylistService playlistService;
    private final Optional<AudioPlayer> audioPlayer;

    private PlayerService playerService;

    public PlayerVerticle(final DatabaseService databaseService,
                          final PlaylistService playlistService) {
        this(databaseService, playlistService, null);
    }

    @VisibleForTesting
    PlayerVerticle(final DatabaseService databaseService,
                   final PlaylistService playlistService,
                   final AudioPlayer player) {
        this.databaseService = databaseService;
        this.playlistService = playlistService;
        this.audioPlayer = Optional.ofNullable(player);
    }

    @Override
    public Future<?> start() {
        final AudioPlayer player = audioPlayer.orElseGet(() -> AudioPlayerFactory.create(config()));
        playerService = PlayerService.create(databaseService, playlistService, player);
        final ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(PlayerVerticle.class.getName())
              .register(PlayerService.class, playerService);
        return Future.succeededFuture();
    }

    @Override
    public Future<?> stop() {
        return playerService.stop();
    }
}
