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

final class ImageCodec {

    /**
     * Keeps the loaded library alive for the lifetime of this binding.
     */
    private final Arena arena = Arena.ofShared();

    private final MethodHandle coverOptimize;
    private final MethodHandle coverFree;

    ImageCodec(final Path library) {
        final SymbolLookup lookup = SymbolLookup.libraryLookup(library, arena);
        final Linker linker = Linker.nativeLinker();
        // int cover_optimize(const uint8_t* data, int len, const int* dims, int dim_count, float quality, uint8_t** out_bufs, size_t* out_lens);
        coverOptimize = linker.downcallHandle(find(lookup, "cover_optimize"),
                                              FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_FLOAT, ADDRESS, ADDRESS));
        // void cover_free(uint8_t* ptr);
        coverFree = linker.downcallHandle(find(lookup, "cover_free"),
                                          FunctionDescriptor.ofVoid(ADDRESS));
    }

    private static MemorySegment find(final SymbolLookup lookup, final String name) {
        return lookup.find(name)
                     .orElseThrow(() -> new IllegalStateException("libimage is missing symbol " + name));
    }

    /**
     * Decode the image once and produce one webp per dimension: aspect-fit
     * scaled, centered on a transparent square canvas of that dimension.
     */
    byte[][] optimize(final byte[] image, final int[] dimensions, final float quality) {
        try (Arena call = Arena.ofConfined()) {
            final MemorySegment input = call.allocateFrom(JAVA_BYTE, image);
            final MemorySegment dims = call.allocateFrom(JAVA_INT, dimensions);
            final MemorySegment outputPointers = call.allocate(ADDRESS, dimensions.length);
            final MemorySegment outputSizes = call.allocate(JAVA_LONG, dimensions.length);
            final int ok = (int) coverOptimize.invokeExact(input, image.length, dims, dimensions.length, quality, outputPointers, outputSizes);
            if (ok == 0) {
                throw new IllegalStateException("Image optimization failed (unsupported format or encoding error)");
            }
            final byte[][] outputs = new byte[dimensions.length][];
            for (int i = 0; i < dimensions.length; i++) {
                final long size = outputSizes.getAtIndex(JAVA_LONG, i);
                final MemorySegment webp = outputPointers.getAtIndex(ADDRESS, i);
                try {
                    outputs[i] = webp.reinterpret(size).toArray(JAVA_BYTE);
                } finally {
                    coverFree.invokeExact(webp);
                }
            }
            return outputs;
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
