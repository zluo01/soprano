# Image bundle: stb_image decode/scale (vendored headers) + static libwebp
# encode, linked with cover_optimization.c into the single /libimage resource.
#
# Not runnable on its own — sourced by the build-<os>.sh entry scripts after
# common.sh, then invoked as: build_libimage <linux|mac|windows>
#
# Reads the same platform knobs as the other stages (LIBWEBP_EXTRA_CONF,
# CROSS_PREFIX) plus $ALLOWED from the entry script for the linux ldd check.

IMAGE_DIR="$NATIVE_DIR/image"

# Static libwebp for the encoder half. Only the encoding core is needed;
# every optional feature and tool is off.
build_libwebp() {
    if ! done_stamp libwebp; then
        fetch "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-$LIBWEBP_VERSION.tar.gz" "$LIBWEBP_SHA256"
        log "Building libwebp"
        ( cd "$SRC/libwebp-$LIBWEBP_VERSION" \
          && ./configure --prefix="$PREFIX" --enable-static --disable-shared --with-pic \
                 ${LIBWEBP_EXTRA_CONF:-} \
                 --disable-libwebpdemux --disable-libwebpmux --disable-libwebpdecoder \
                 --disable-png --disable-jpeg --disable-tiff --disable-gif --disable-wic \
                 --disable-sdl --disable-gl >/dev/null \
          && make -j"$JOBS" >/dev/null && make install >/dev/null )
        mark_done libwebp
    fi
}

# dlopen the built bundle and run the full decode -> scale -> encode pipeline
# on an embedded 2x2 PNG — verifies stb and the webp encoder end to end.
# (Host-run platforms only.)
run_image_load_check() { # $1 = library path
    python3 - "$1" <<'EOF'
import ctypes, struct, sys, zlib

def chunk(t, d):
    return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d))

raw = b"".join(b"\x00" + bytes([255, 0, 0, 255] * 2) for _ in range(2))
png = (b"\x89PNG\r\n\x1a\n"
       + chunk(b"IHDR", struct.pack(">IIBBBBB", 2, 2, 8, 6, 0, 0, 0))
       + chunk(b"IDAT", zlib.compress(raw))
       + chunk(b"IEND", b""))

lib = ctypes.CDLL(sys.argv[1])
lib.cover_optimize.restype = ctypes.c_int
lib.cover_optimize.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.POINTER(ctypes.c_int),
                               ctypes.c_int, ctypes.c_float,
                               ctypes.POINTER(ctypes.c_void_p), ctypes.POINTER(ctypes.c_size_t)]
lib.cover_free.argtypes = [ctypes.c_void_p]

dims = (ctypes.c_int * 2)(64, 32)
bufs = (ctypes.c_void_p * 2)()
lens = (ctypes.c_size_t * 2)()
ok = lib.cover_optimize(png, len(png), dims, 2, 75.0, bufs, lens)
assert ok == 1, "cover_optimize failed"
for i in range(2):
    assert lens[i] > 0, f"empty output {i}"
    assert ctypes.string_at(bufs[i], 4) == b"RIFF", f"output {i} is not webp"
    lib.cover_free(bufs[i])
EOF
    log "libimage load check passed: decode -> scale -> encode produces webp."
}

build_libimage() { # $1 = linux|mac|windows
    build_libwebp

    local dest="$OUT/libimage"
    log "Linking image bundle"
    case "$1" in
        linux)
            gcc $CFLAGS -shared -o "$dest" \
                -I"$IMAGE_DIR" -I"$PREFIX/include" "$IMAGE_DIR/cover_optimization.c" \
                "$PREFIX/lib/libwebp.a" "$PREFIX/lib/libsharpyuv.a" -lm -lpthread
            strip --strip-unneeded "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            local unexpected
            unexpected="$(ldd "$dest" | awk '{print $1}' | grep -Ev "$ALLOWED" || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: unexpected dynamic dependencies in image bundle:"
                echo "$unexpected"
                exit 1
            fi
            run_image_load_check "$dest"
            ;;
        mac)
            cc $CFLAGS -dynamiclib -o "$dest" \
                -I"$IMAGE_DIR" -I"$PREFIX/include" "$IMAGE_DIR/cover_optimization.c" \
                "$PREFIX/lib/libwebp.a" "$PREFIX/lib/libsharpyuv.a" -lm
            strip -x "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            local unexpected
            unexpected="$(otool -L "$dest" | tail -n +2 | awk '{print $1}' \
                | grep -v "libimage" \
                | grep -Ev '^(/usr/lib/|/System/Library/)' || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: unexpected dynamic dependencies in image bundle:"
                echo "$unexpected"
                exit 1
            fi
            run_image_load_check "$dest"
            ;;
        windows)
            # mingw appends .exe to extensionless output names; link with a
            # .dll name and rename to the fixed resource name afterwards.
            "${CROSS_PREFIX}gcc" $CFLAGS -shared -static -o "$WORK/libimage.dll" \
                -I"$IMAGE_DIR" -I"$PREFIX/include" "$IMAGE_DIR/cover_optimization.c" \
                "$PREFIX/lib/libwebp.a" "$PREFIX/lib/libsharpyuv.a"
            "${CROSS_PREFIX}strip" --strip-unneeded "$WORK/libimage.dll"
            mv "$WORK/libimage.dll" "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            local unexpected
            unexpected="$("${CROSS_PREFIX}objdump" -p "$dest" | grep "DLL Name" \
                | grep -Ei "libwinpthread|libgcc|libstdc|libssp" || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: non-system DLL dependencies in image bundle:"
                echo "$unexpected"
                exit 1
            fi
            ;;
        *)
            echo "ERROR: unknown image bundle target: $1"
            exit 1
            ;;
    esac

    # stored gzipped in the resource dir; extracted through GZIPInputStream at
    # runtime (BundledLibrary). -n keeps the output byte-stable across rebuilds.
    gzip -9 -n -f "$dest"
    log "Compressed bundle: $dest.gz ($(du -h "$dest.gz" | cut -f1))"
}
