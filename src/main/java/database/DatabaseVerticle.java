package database;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import static config.ServerConfig.DATABASE_CONFIG;
import static config.ServerConfig.DATABASE_FILE_PATH;

public class DatabaseVerticle extends VerticleBase {
    @Override
    public Future<?> start() {
        final String url = config().getString(DATABASE_CONFIG, DATABASE_FILE_PATH);

        final JDBCConnectOptions connectOptions = new JDBCConnectOptions()
                .setJdbcUrl("jdbc:sqlite:" + url);
        final Pool client = JDBCPool.pool(vertx, connectOptions, new PoolOptions());
        final DatabaseService databaseService = DatabaseService.create(client);
        return databaseService.initialization()
                              .compose(__ -> {
                                  final ServiceBinder binder = new ServiceBinder(vertx);
                                  binder.setAddress(DatabaseVerticle.class.getName())
                                        .register(DatabaseService.class, databaseService);
                                  return Future.succeededFuture();
                              });
    }
}
