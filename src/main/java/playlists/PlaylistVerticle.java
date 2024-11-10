package playlists;

import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.serviceproxy.ServiceBinder;

public class PlaylistVerticle extends AbstractVerticle {
    @Override
    public void start(final Promise<Void> promise) {
        final FileSystem fileSystem = vertx.fileSystem();
        final DatabaseService databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        final PlaylistService playlistService = PlaylistService.create(databaseService, fileSystem);
        final ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(PlaylistVerticle.class.getName())
              .register(PlaylistService.class, playlistService);
        promise.complete();
    }
}
