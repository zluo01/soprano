package helper;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class TestUtils {
    private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);

    private TestUtils() {
    }

    public static Buffer readFileAsBuffer(String file) {
        try (InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(file)) {
            return Buffer.buffer(Objects.requireNonNull(is).readAllBytes());
        } catch (IOException e) {
            LOGGER.error(e);
            return Buffer.buffer();
        }
    }
}
