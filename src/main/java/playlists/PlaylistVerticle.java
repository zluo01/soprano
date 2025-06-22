package playlists;

import database.DatabaseService;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.file.FileSystem;
import io.vertx.serviceproxy.ServiceBinder;

public class PlaylistVerticle extends VerticleBase {
    private final DatabaseService databaseService;

    public PlaylistVerticle(final DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public Future<?> start() {
        final FileSystem fileSystem = vertx.fileSystem();
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
