package helper;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ServiceProxyBuilder;

public final class ServiceHelper {
    private ServiceHelper() {
    }

    public static <T> T createServiceProxy(final Vertx vertx,
                                           final Class<? extends AbstractVerticle> proxy,
                                           final Class<T> tClass) {
        return new ServiceProxyBuilder(vertx).setAddress(proxy.getName()).build(tClass);
    }
}
