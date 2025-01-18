package images;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.regex.Pattern;

public class StaticImageHandlerImpl implements StaticImageHandler {
    private static final Logger LOGGER = LogManager.getLogger(StaticImageHandlerImpl.class);

    private static final Pattern IMAGE_PATH_PATTERN = Pattern.compile("^/covers/(undefined|null|-?\\d+)(_\\d+x\\d+)?\\.webp$");

    private static final Buffer FALLBACK_IMAGE;

    static {
        final String sourcePath = "images/default.avif";
        try (InputStream stream = StaticImageHandlerImpl.class.getClassLoader().getResourceAsStream(sourcePath)) {
            FALLBACK_IMAGE = Buffer.buffer(Objects.requireNonNull(stream).readAllBytes());
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Fail to load default image cover.", e);
        }
    }

    private final String rootPath;

    public StaticImageHandlerImpl(final String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void handle(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
            context.response().setStatusCode(HttpResponseStatus.METHOD_NOT_ALLOWED.code()).end();
        } else {
            final String uriDecodedPath = URIDecoder.decodeURIComponent(context.normalizedPath(), false);
            if (uriDecodedPath == null) {
                LOGGER.warn("Invalid path: {}", context.request().path());
                context.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
                return;
            }

            final String path = HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));
            if (IMAGE_PATH_PATTERN.matcher(path).matches()) {
                final FileSystem fs = context.vertx().fileSystem();
                sendImageFile(context, fs, path);
            } else {
                LOGGER.warn("Invalid image path: {}", path);
                context.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            }
        }
    }

    private void sendImageFile(final RoutingContext context,
                               final FileSystem fileSystem,
                               final String path) {

        final String filePath = getFile(path, context);
        fileSystem.exists(filePath)
                  .onSuccess(exists -> {
                      if (!exists) { // serve fallback image
                          final String alternativeFilePath = getAlternativeFile(path, context);
                          fileSystem.exists(alternativeFilePath)
                                    .onSuccess(existsResult -> {
                                        if (existsResult) {
                                            sendImageFile(alternativeFilePath, context);
                                        } else {
                                            sendDefaultCoverImage(context);
                                        }
                                    })
                                    .onFailure(error -> {
                                        LOGGER.error("Error sending image file", error);
                                        context.fail(error);
                                    });
                      } else {
                          sendImageFile(filePath, context);
                      }
                  })
                  .onFailure(error -> {
                      LOGGER.error("Error sending image file", error);
                      context.fail(error);
                  });
    }

    private String getAlternativeFile(final String path, final RoutingContext context) {
        final String originFileName = Utils.pathOffset(path, context);
        final String alternativeFileName = originFileName.replaceAll("_\\d+x\\d+", "");
        return this.rootPath + alternativeFileName;
    }

    private String getFile(final String path, final RoutingContext context) {
        return this.rootPath + Utils.pathOffset(path, context);
    }

    private void sendDefaultCoverImage(final RoutingContext context) {
        context.response()
               .putHeader(HttpHeaders.CONTENT_TYPE, "image/avif")
               .putHeader(HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=86400")
               .send(FALLBACK_IMAGE);
    }

    private void sendImageFile(final String path, final RoutingContext context) {
        context.response()
               .putHeader(HttpHeaders.CONTENT_TYPE, "image/webp")
               .putHeader(HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=86400")
               .sendFile(path);
    }
}
