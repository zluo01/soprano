package worker.eventBus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class ArtworkPayloadCodec implements MessageCodec<ArtworkPayload, ArtworkPayload> {

    @Override
    public void encodeToWire(final Buffer buffer, final ArtworkPayload payload) {
        buffer.appendString(payload.destination());
        buffer.appendInt(payload.imageData().length);
        buffer.appendBytes(payload.imageData());
    }

    @Override
    public ArtworkPayload decodeFromWire(final int pos, final Buffer buffer) {
        int p = pos;

        final String destination = buffer.getString(p, p + buffer.length());
        p += destination.length();

        final int dataLength = buffer.getInt(p);
        p += 4; // Jump 4 because getInt() == 4 bytes

        final byte[] data = buffer.getBytes(p, p + dataLength);
        return new ArtworkPayload(destination, data);
    }

    @Override
    public ArtworkPayload transform(final ArtworkPayload payload) {
        return payload; // No transformation, just return the same object
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        // Always -1
        return -1;
    }
}

