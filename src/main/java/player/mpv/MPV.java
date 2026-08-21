package player.mpv;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * <a href="https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h">Reference C Implementation</a>
 */
public final class MPV {

    // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1585-L1630
    private static final StructLayout MPV_EVENT = MemoryLayout.structLayout(
            JAVA_INT.withName("event_id"),
            JAVA_INT.withName("error"),
            JAVA_LONG.withName("reply_userdata"),
            ADDRESS.withName("data"));
    private static final long EVENT_ID_OFFSET = MPV_EVENT.byteOffset(groupElement("event_id"));
    private static final long EVENT_DATA_OFFSET = MPV_EVENT.byteOffset(groupElement("data"));

    // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1508-L1550
    private static final StructLayout MPV_EVENT_END_FILE = MemoryLayout.structLayout(
            JAVA_INT.withName("reason"),
            JAVA_INT.withName("error"),
            JAVA_LONG.withName("playlist_entry_id"),
            JAVA_LONG.withName("playlist_insert_id"),
            JAVA_INT.withName("playlist_insert_num_entries"),
            MemoryLayout.paddingLayout(4));

    // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1465-L1498
    private static final long END_FILE_REASON_OFFSET = MPV_EVENT_END_FILE.byteOffset(groupElement("reason"));

    /**
     * Keeps the loaded library alive for the lifetime of this binding.
     */
    private final Arena arena = Arena.ofShared();

    private final MethodHandle create;
    private final MethodHandle initialize;
    private final MethodHandle setOptionString;
    private final MethodHandle setPropertyString;
    private final MethodHandle getPropertyString;
    private final MethodHandle command;
    private final MethodHandle waitEvent;
    private final MethodHandle free;
    private final MethodHandle terminateDestroy;

    public MPV(final Path library) {
        final SymbolLookup lookup = SymbolLookup.libraryLookup(library, arena);
        final Linker linker = Linker.nativeLinker();
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L481
        create = linker.downcallHandle(find(lookup, "mpv_create"),
                                       FunctionDescriptor.of(ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L503
        initialize = linker.downcallHandle(find(lookup, "mpv_initialize"),
                                           FunctionDescriptor.of(JAVA_INT, ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L892
        setOptionString = linker.downcallHandle(find(lookup, "mpv_set_option_string"),
                                                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1086
        setPropertyString = linker.downcallHandle(find(lookup, "mpv_set_property_string"),
                                                  FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1150
        getPropertyString = linker.downcallHandle(find(lookup, "mpv_get_property_string"),
                                                  FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L908
        command = linker.downcallHandle(find(lookup, "mpv_command"),
                                        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1720
        waitEvent = linker.downcallHandle(find(lookup, "mpv_wait_event"),
                                          FunctionDescriptor.of(ADDRESS.withTargetLayout(MPV_EVENT), ADDRESS, JAVA_DOUBLE));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L399
        free = linker.downcallHandle(find(lookup, "mpv_free"),
                                     FunctionDescriptor.ofVoid(ADDRESS));
        // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L542
        terminateDestroy = linker.downcallHandle(find(lookup, "mpv_terminate_destroy"),
                                                 FunctionDescriptor.ofVoid(ADDRESS));
    }

    private static MemorySegment find(final SymbolLookup lookup, final String name) {
        return lookup.find(name)
                     .orElseThrow(() -> new IllegalStateException("libmpv is missing symbol " + name));
    }

    /**
     * Returns the opaque mpv_handle, or a NULL segment on failure.
     */
    public MemorySegment mpv_create() {
        try {
            return (MemorySegment) create.invokeExact();
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public int mpv_initialize(final MemorySegment handle) {
        try {
            return (int) initialize.invokeExact(handle);
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public int mpv_set_option_string(final MemorySegment handle, final String name, final String value) {
        try (Arena call = Arena.ofConfined()) {
            return (int) setOptionString.invokeExact(handle, call.allocateFrom(name), call.allocateFrom(value));
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public int mpv_set_property_string(final MemorySegment handle, final String name, final String value) {
        try (Arena call = Arena.ofConfined()) {
            return (int) setPropertyString.invokeExact(handle, call.allocateFrom(name), call.allocateFrom(value));
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public String mpv_get_property_string(final MemorySegment handle, final String name) {
        try (Arena call = Arena.ofConfined()) {
            final MemorySegment value = (MemorySegment) getPropertyString.invokeExact(handle, call.allocateFrom(name));
            if (value.address() == 0) {
                return null;
            }
            try {
                return value.reinterpret(Long.MAX_VALUE).getString(0);
            } finally {
                free.invokeExact(value);
            }
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public int mpv_command(final MemorySegment handle, final String[] args) {
        try (Arena call = Arena.ofConfined()) {
            final MemorySegment argv = call.allocate(ADDRESS, args.length + 1);
            for (int i = 0; i < args.length; i++) {
                argv.setAtIndex(ADDRESS, i, call.allocateFrom(args[i]));
            }
            argv.setAtIndex(ADDRESS, args.length, MemorySegment.NULL);
            return (int) command.invokeExact(handle, argv);
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public void mpv_terminate_destroy(final MemorySegment handle) {
        try {
            terminateDestroy.invokeExact(handle);
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    public Event mpv_wait_event(final MemorySegment handle, final double timeout) {
        try {
            final MemorySegment event = (MemorySegment) waitEvent.invokeExact(handle, timeout);
            final int eventId = event.get(JAVA_INT, EVENT_ID_OFFSET);

            int endFileReason = -1;
            if (eventId == MPVEventId.MPV_EVENT_END_FILE) {
                endFileReason = event.get(ADDRESS, EVENT_DATA_OFFSET)
                                     .reinterpret(MPV_EVENT_END_FILE.byteSize())
                                     .get(JAVA_INT, END_FILE_REASON_OFFSET);
            }
            return new Event(eventId, endFileReason);
        } catch (final Throwable t) {
            throw propagate(t);
        }
    }

    /**
     *
     * @param eventId       mpv event id
     * @param endFileReason mpv event end file id. -1 if not ending.
     */
    public record Event(int eventId, int endFileReason) {

        public boolean isNone() {
            return eventId == MPVEventId.MPV_EVENT_NONE;
        }

        public boolean isFileLoaded() {
            return eventId == MPVEventId.MPV_EVENT_FILE_LOADED;
        }

        /**
         * True when playback stopped due to file reached its end.
         */
        public boolean isEndOfFile() {
            return eventId == MPVEventId.MPV_EVENT_END_FILE
                   && endFileReason == MPVEndFileReason.MPV_END_FILE_REASON_EOF;
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
