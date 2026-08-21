package collector;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * <a href="https://developers.google.com/speed/webp/docs/api#simple_encoding_api">libwebp simple encoding API</a>
 */
final class WebP {

    /**
     * Keeps the loaded library alive for the lifetime of this binding.
     */
    private final Arena arena = Arena.ofShared();

    private final MethodHandle encodeRGBA;
    private final MethodHandle webpFree;

    WebP(final Path library) {
        final SymbolLookup lookup = SymbolLookup.libraryLookup(library, arena);
        final Linker linker = Linker.nativeLinker();
        // size_t WebPEncodeRGBA(const uint8_t* rgba, int width, int height, int stride, float quality_factor, uint8_t** output);
        encodeRGBA = linker.downcallHandle(find(lookup, "WebPEncodeRGBA"),
                                           FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_FLOAT, ADDRESS));
        // void WebPFree(void* ptr);
        webpFree = linker.downcallHandle(find(lookup, "WebPFree"),
                                         FunctionDescriptor.ofVoid(ADDRESS));
    }

    private static MemorySegment find(final SymbolLookup lookup, final String name) {
        return lookup.find(name)
                     .orElseThrow(() -> new IllegalStateException("libwebp is missing symbol " + name));
    }

    byte[] encodeRGBA(final byte[] rgba, final int width, final int height, final float quality) {
        try (Arena call = Arena.ofConfined()) {
            final MemorySegment input = call.allocateFrom(JAVA_BYTE, rgba);
            final MemorySegment outputPointer = call.allocate(ADDRESS);
            final long size = (long) encodeRGBA.invokeExact(input, width, height, width * 4, quality, outputPointer);
            if (size == 0) {
                throw new IllegalStateException("WebP encoding failed");
            }
            final MemorySegment output = outputPointer.get(ADDRESS, 0);
            try {
                return output.reinterpret(size).toArray(JAVA_BYTE);
            } finally {
                webpFree.invokeExact(output);
            }
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    private static RuntimeException propagate(final Throwable t) {
        if (t instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }
}
