package server;

import config.ServerConfig;
import database.DatabaseService;
import database.DatabaseVerticle;
import graphql.GraphQL;
import helper.ServiceHelper;
import images.StaticImageHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import player.PlayerService;
import player.PlayerVerticle;
import playlists.PlaylistService;
import playlists.PlaylistVerticle;

import static config.ServerConfig.enableGraphQLDebug;
import static config.ServerConfig.isWebUiEnabled;

public final class WebServerVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(WebServerVerticle.class);

    private static final int DEFAULT_PORT = 6868;

    private DatabaseService databaseService;
    private PlaylistService playlistService;
    private PlayerService playerService;
    private Router router;

    @Override
    public void start(final Promise<Void> promise) {
        initConfig(promise)
                .compose(this::setupServices)
                .compose(this::setupRoutes)
                .compose(this::startServer)
                .onComplete(promise);
    }

    private Future<Startup> initConfig(final Promise<Void> bootstrap) {
        return vertx.fileSystem().readFile("schemas/main.graphql")
                    .map(o -> new Startup(bootstrap, o.toString()));
    }

    private Future<Startup> setupServices(final Startup startup) {
        databaseService = ServiceHelper.createServiceProxy(vertx, DatabaseVerticle.class, DatabaseService.class);
        playlistService = ServiceHelper.createServiceProxy(vertx, PlaylistVerticle.class, PlaylistService.class);
        playerService = ServiceHelper.createServiceProxy(vertx, PlayerVerticle.class, PlayerService.class);
        router = Router.router(vertx);
        return Future.succeededFuture(startup);
    }

    private Future<Startup> setupRoutes(final Startup startup) {
        final boolean enableWebUI = isWebUiEnabled(config());
        final boolean enableDebugConsole = enableGraphQLDebug(config());

        final GraphQL graphQL = GraphQLInitializer.setup(startup.schema,
                                                         databaseService,
                                                         playlistService,
                                                         playerService,
                                                         vertx.eventBus());

        router.route().handler(BodyHandler.create());

        if (enableWebUI) {
            router.get().handler(StaticHandler.create());
        }

        router.route("/covers/*").handler(StaticImageHandler.create(ServerConfig.COVER_PATH));

        router.route("/graphql").handler(GraphQLHandler.create(graphQL));

        final GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
                .setEnabled(enableDebugConsole);

        final GraphiQLHandler handler = GraphiQLHandler.create(vertx, options);

        router.route("/graphiql*").subRouter(handler.router());

        return Future.succeededFuture(startup);
    }

    private Future<Void> startServer(final Startup startup) {
        final Promise<Void> promise = Promise.promise();
        vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
             .requestHandler(router)
             .listen(DEFAULT_PORT)
             .onSuccess(__ -> {
                 LOGGER.info("HTTP server running on port {}", DEFAULT_PORT);
                 promise.complete();
             })
             .onFailure(startup.bootstrap::fail);
        return promise.future();
    }

    private record Startup(Promise<Void> bootstrap, String schema) {
    }
}
