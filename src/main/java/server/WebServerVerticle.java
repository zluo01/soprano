package server;

import config.ServerConfig;
import database.DatabaseService;
import graphql.GraphQL;
import helper.ServiceHelper;
import images.StaticImageHandler;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.ws.GraphQLWSHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import player.PlayerService;
import player.PlayerVerticle;
import playlists.PlaylistService;

import static config.ServerConfig.enableGraphQLDebug;
import static config.ServerConfig.isWebUiEnabled;

public final class WebServerVerticle extends VerticleBase {
    private static final Logger LOGGER = LogManager.getLogger(WebServerVerticle.class);

    private static final int DEFAULT_PORT = 6868;

    private final DatabaseService databaseService;
    private final PlaylistService playlistService;
    private PlayerService playerService;
    private Router router;

    public WebServerVerticle(final DatabaseService databaseService, final PlaylistService playlistService) {
        this.databaseService = databaseService;
        this.playlistService = playlistService;
    }

    @Override
    public Future<?> start() {
        return initConfig().compose(this::setupServices)
                           .compose(this::setupRoutes)
                           .compose(this::startServer);
    }

    private Future<StartupContext> initConfig() {
        return vertx.fileSystem().readFile("schemas/main.graphql")
                    .map(o -> new StartupContext(o.toString()));
    }

    private Future<StartupContext> setupServices(final StartupContext startup) {
        playerService = ServiceHelper.createServiceProxy(vertx, PlayerVerticle.class, PlayerService.class);
        router = Router.router(vertx);
        return Future.succeededFuture(startup);
    }

    private Future<StartupContext> setupRoutes(final StartupContext startup) {
        final boolean enableWebUI = isWebUiEnabled(config());
        final boolean enableDebugConsole = enableGraphQLDebug(config());

        final GraphQL graphQL = GraphQLInitializer.setup(startup.schema,
                                                         databaseService,
                                                         playlistService,
                                                         playerService,
                                                         vertx.eventBus());

        router.route().handler(BodyHandler.create());

        if (enableWebUI) {
            router.get("/").handler(ctx -> ctx.response()
                    .putHeader("Content-Type", "text/html")
                    .putHeader("Cache-Control", "public, max-age=0, must-revalidate")
                    .sendFile("webroot/index.html"));

            router.get("/sw.js").handler(ctx -> ctx.response()
                    .putHeader("Content-Type", "application/javascript")
                    .putHeader("Cache-Control", "public, max-age=0, must-revalidate")
                    .sendFile("webroot/sw.js"));

            router.get("/manifest.webmanifest").handler(ctx -> ctx.response()
                    .putHeader("Content-Type", "application/manifest+json")
                    .sendFile("webroot/manifest.webmanifest"));

            router.get().handler(StaticHandler.create().setCachingEnabled(true));
        }

        router.route("/covers/*").handler(StaticImageHandler.create(ServerConfig.COVER_PATH));

        router.route("/graphql")
              .handler(GraphQLWSHandler.create(graphQL))
              .handler(GraphQLHandler.create(graphQL));

        final GraphiQLHandlerOptions options = new GraphiQLHandlerOptions()
                .setEnabled(enableDebugConsole);

        final GraphiQLHandler handler = GraphiQLHandler.create(vertx, options);

        router.route("/graphiql*").subRouter(handler.router());

        return Future.succeededFuture(startup);
    }

    private Future<HttpServer> startServer(final StartupContext startup) {
        return vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
                    .requestHandler(router)
                    .listen(DEFAULT_PORT)
                    .onSuccess(__ -> LOGGER.info("HTTP server running on port {}", DEFAULT_PORT))
                    .onFailure(LOGGER::fatal);
    }

    private record StartupContext(String schema) {
    }
}
