package player;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.serviceproxy.ServiceBinder;

public class PlayerVerticle extends VerticleBase {

    private final DatabaseService databaseService;

    public PlayerVerticle(final DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Future<?> start() {
        final MPVInstance instance = MPVInstance.create(config());

        final PlayerService playlistService = PlayerService.create(databaseService, instance);
        final ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(PlayerVerticle.class.getName())
              .register(PlayerService.class, playlistService);
        return Future.succeededFuture();
    }
}
