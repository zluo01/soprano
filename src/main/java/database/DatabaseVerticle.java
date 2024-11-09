package database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.serviceproxy.ServiceBinder;

import static config.ServerConfig.DATABASE_CONFIG;
import static config.ServerConfig.DATABASE_FILE_PATH;

public class DatabaseVerticle extends AbstractVerticle {
    @Override
    public void start(final Promise<Void> promise) {
        final String url = config().getString(DATABASE_CONFIG, DATABASE_FILE_PATH);
        final JsonObject config = new JsonObject().put("url", "jdbc:sqlite:" + url)
                                                  .put("driver_class", "org.sqlite.JDBC");

        final JDBCPool client = JDBCPool.pool(vertx, config);
        final DatabaseService databaseService = DatabaseService.create(client);
        databaseService.initialization()
                       .onSuccess(__ -> {
                           final ServiceBinder binder = new ServiceBinder(vertx);
                           binder.setAddress(DatabaseVerticle.class.getName())
                                 .register(DatabaseService.class, databaseService);
                           promise.complete();
                       })
                       .onFailure(promise::fail);
    }
}
