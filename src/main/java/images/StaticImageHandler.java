package images;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface StaticImageHandler extends Handler<RoutingContext> {
    static StaticImageHandler create(final String rootPath) {
        return new StaticImageHandlerImpl(rootPath);
    }
}
