package player;

import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

public class PlayerVerticle extends AbstractVerticle {
    @Override
    public void start(final Promise<Void> promise) {
        final MPVInstance instance = MPVInstance.create(config());

        final DatabaseService databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        final PlayerService playlistService = PlayerService.create(databaseService, instance);
        final ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(PlayerVerticle.class.getName())
              .register(PlayerService.class, playlistService);
        promise.complete();
    }
}
