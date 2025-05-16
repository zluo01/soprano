package playlists;

import database.DatabaseService;
import database.DatabaseVerticle;
import helper.ServiceHelper;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.file.FileSystem;
import io.vertx.serviceproxy.ServiceBinder;

public class PlaylistVerticle extends VerticleBase {
    @Override
    public Future<?> start() {
        final FileSystem fileSystem = vertx.fileSystem();
        final DatabaseService databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);

        final PlaylistService playlistService = PlaylistService.create(databaseService, fileSystem);

        return playlistService.validatePlaylists()
                              .compose(__ -> {
                                  final ServiceBinder binder = new ServiceBinder(vertx);
                                  binder.setAddress(PlaylistVerticle.class.getName())
                                        .register(PlaylistService.class, playlistService);
                                  return Future.succeededFuture();
                              });
    }
}
