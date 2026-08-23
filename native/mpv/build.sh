# mpv bundle: audio-only libmpv with FFmpeg, libass, libplacebo, freetype,
# harfbuzz, fribidi and zlib statically linked in, produced as the single
# /libmpv resource.
#
# Not runnable on its own — sourced by the build-<os>.sh entry scripts after
# common.sh, then invoked as: build_libmpv <linux|mac|windows>
#
# Reads the platform knobs documented in common.sh (CXX_RUNTIME_LIB,
# MPV_OS_ARGS, MPV_LDFLAGS, MPV_PC_LIBDIR, MESON_CROSS_ARGS,
# LIBASS_EXTRA_CONF, FFMPEG_TARGET_ARGS, CROSS_PREFIX) plus $ALLOWED from the
# entry script for the linux ldd check.

meson_build() { # meson_build <srcdir> [extra meson args...]
    local dir="$1"; shift
    ( cd "$dir" \
      && rm -rf build \
      && meson setup build --prefix="$PREFIX" --libdir=lib --buildtype=release \
             -Ddefault_library=static --prefer-static \
             -Dauto_features=disabled ${MESON_CROSS_ARGS:-} "$@" \
      && ninja -C build -j"$JOBS" install )
}

# ------------------------------------------------------------- dependencies

build_zlib() {
    if ! done_stamp zlib; then
        fetch "https://github.com/madler/zlib/releases/download/v$ZLIB_VERSION/zlib-$ZLIB_VERSION.tar.xz" "$ZLIB_SHA256"
        log "Building zlib"
        if [ -n "${CROSS_PREFIX:-}" ]; then
            ( cd "$SRC/zlib-$ZLIB_VERSION" \
              && make -f win32/Makefile.gcc PREFIX="$CROSS_PREFIX" libz.a >/dev/null \
              && cp libz.a "$PREFIX/lib/" && cp zlib.h zconf.h "$PREFIX/include/" )
            cat > "$PREFIX/lib/pkgconfig/zlib.pc" <<EOF
prefix=$PREFIX
libdir=\${prefix}/lib
includedir=\${prefix}/include

Name: zlib
Description: zlib compression library
Version: $ZLIB_VERSION
Libs: -L\${libdir} -lz
Cflags: -I\${includedir}
EOF
        else
            ( cd "$SRC/zlib-$ZLIB_VERSION" \
              && ./configure --prefix="$PREFIX" --static >/dev/null \
              && make -j"$JOBS" >/dev/null && make install >/dev/null )
        fi
        mark_done zlib
    fi
}

build_freetype() {
    if ! done_stamp freetype; then
        fetch "https://download.savannah.gnu.org/releases/freetype/freetype-$FREETYPE_VERSION.tar.xz" "$FREETYPE_SHA256"
        log "Building freetype"
        meson_build "$SRC/freetype-$FREETYPE_VERSION" -Dzlib=internal
        mark_done freetype
    fi
}

build_fribidi() {
    if ! done_stamp fribidi; then
        fetch "https://github.com/fribidi/fribidi/releases/download/v$FRIBIDI_VERSION/fribidi-$FRIBIDI_VERSION.tar.xz" "$FRIBIDI_SHA256"
        log "Building fribidi"
        meson_build "$SRC/fribidi-$FRIBIDI_VERSION" -Ddocs=false -Dtests=false
        mark_done fribidi
    fi
}

build_harfbuzz() {
    if ! done_stamp harfbuzz; then
        fetch "https://github.com/harfbuzz/harfbuzz/releases/download/$HARFBUZZ_VERSION/harfbuzz-$HARFBUZZ_VERSION.tar.xz" "$HARFBUZZ_SHA256"
        log "Building harfbuzz"
        meson_build "$SRC/harfbuzz-$HARFBUZZ_VERSION" -Dfreetype=enabled -Dtests=disabled -Ddocs=disabled
        # harfbuzz is C++ but its .pc does not declare the C++ runtime; mpv
        # links with the C driver, so declare it explicitly or symbols like
        # std::from_chars stay unresolved in the final library.
        sed -i.bak "s/^Libs: .*/& $CXX_RUNTIME_LIB/" "$PREFIX/lib/pkgconfig/harfbuzz.pc" \
            && rm -f "$PREFIX/lib/pkgconfig/harfbuzz.pc.bak"
        mark_done harfbuzz
    fi
}

# Hard-required by mpv even for audio-only builds (subtitle renderer).
# All system font providers are disabled: no font discovery is needed.
build_libass() {
    if ! done_stamp libass; then
        fetch "https://github.com/libass/libass/releases/download/$LIBASS_VERSION/libass-$LIBASS_VERSION.tar.xz" "$LIBASS_SHA256"
        log "Building libass"
        ( cd "$SRC/libass-$LIBASS_VERSION" \
          && ./configure --prefix="$PREFIX" --enable-static --disable-shared --with-pic \
                 --disable-fontconfig --disable-libunibreak \
                 --disable-require-system-font-provider ${LIBASS_EXTRA_CONF:-} >/dev/null \
          && make -j"$JOBS" >/dev/null && make install >/dev/null )
        mark_done libass
    fi
}

# Hard-required by mpv. Built with every GPU backend disabled: it only has to
# exist at link time, none of its rendering paths are reachable in this build.
build_libplacebo() {
    if ! done_stamp libplacebo; then
        if [ ! -d "$SRC/libplacebo-v$LIBPLACEBO_VERSION" ]; then
            log "Cloning libplacebo v$LIBPLACEBO_VERSION"
            git clone --recursive --depth 1 --branch "v$LIBPLACEBO_VERSION" \
                https://code.videolan.org/videolan/libplacebo.git "$SRC/libplacebo-v$LIBPLACEBO_VERSION"
        fi
        log "Building libplacebo"
        meson_build "$SRC/libplacebo-v$LIBPLACEBO_VERSION" -Ddemos=false -Dtests=false
        mark_done libplacebo
    fi
}

# Trimmed to audio: file access, audio demuxers/decoders/parsers, resampling.
# No encoders, muxers, video decoders, network, or hardware acceleration.
# libswscale and libavfilter are kept because mpv requires them at link time.
build_ffmpeg() {
    if ! done_stamp ffmpeg; then
        fetch "https://ffmpeg.org/releases/ffmpeg-$FFMPEG_VERSION.tar.xz" "$FFMPEG_SHA256"
        log "Building ffmpeg (audio-only)"
        ( cd "$SRC/ffmpeg-$FFMPEG_VERSION" \
          && ./configure --prefix="$PREFIX" \
                 --enable-static --disable-shared --enable-pic \
                 --disable-programs --disable-doc --disable-debug \
                 --disable-avdevice \
                 --disable-network --disable-autodetect \
                 --enable-zlib \
                 --pkg-config=pkg-config --pkg-config-flags=--static \
                 --extra-cflags="-I$PREFIX/include" \
                 --extra-ldflags="-L$PREFIX/lib" \
                 ${FFMPEG_TARGET_ARGS:-} \
                 --disable-everything \
                 --enable-protocol=file,pipe,fd \
                 --enable-demuxer=aac,ac3,aiff,ape,asf,flac,matroska,mov,mp3,mpc,mpc8,ogg,tak,tta,wav,wv,dsf \
                 --enable-decoder=aac,ac3,alac,ape,flac,mp1float,mp2float,mp3float,opus,vorbis,wavpack,wmav1,wmav2,wmalossless,wmapro,tak,tta,mpc7,mpc8,dsd_lsbf,dsd_msbf,dsd_lsbf_planar,dsd_msbf_planar,pcm_alaw,pcm_mulaw,pcm_f32be,pcm_f32le,pcm_f64be,pcm_f64le,pcm_s16be,pcm_s16le,pcm_s24be,pcm_s24le,pcm_s32be,pcm_s32le,pcm_s8,pcm_u8 \
                 --enable-parser=aac,ac3,flac,mpegaudio,opus,vorbis,tak \
                 --enable-filter=aresample,aformat,anull,atrim,volume \
          && make -j"$JOBS" >/dev/null && make install >/dev/null )
        mark_done ffmpeg
    fi
}

# libmpv only (no cplayer); the audio output comes from MPV_OS_ARGS.
build_mpv() {
    if ! done_stamp mpv; then
        fetch "https://github.com/mpv-player/mpv/archive/refs/tags/v$MPV_VERSION.tar.gz" "$MPV_SHA256"
        log "Building mpv (libmpv, audio-only)"
        ( cd "$SRC/mpv-$MPV_VERSION" \
          && rm -rf build \
          && PKG_CONFIG_LIBDIR="${MPV_PC_LIBDIR:-$PKG_CONFIG_LIBDIR}" \
             LDFLAGS="${MPV_LDFLAGS:-}" \
             meson setup build --prefix="$PREFIX" --libdir=lib --buildtype=release \
                 -Ddefault_library=shared --prefer-static \
                 -Dauto_features=disabled ${MESON_CROSS_ARGS:-} \
                 -Dlibmpv=true -Dcplayer=false -Dgl=disabled -Dlua=disabled \
                 "${MPV_OS_ARGS[@]}" \
          && ninja -C build -j"$JOBS" install )
        mark_done mpv
    fi
}

# ------------------------------------------------------------------ checks

# dlopen the built library and bring up an mpv instance — verifies the
# trimmed build is functionally coherent, not merely linkable. (Host-run
# platforms only; cross builds are verified on the target instead.)
run_load_check() { # $1 = library path
    python3 - "$1" <<'EOF'
import ctypes, sys
lib = ctypes.CDLL(sys.argv[1])
lib.mpv_create.restype = ctypes.c_void_p
lib.mpv_initialize.argtypes = [ctypes.c_void_p]
lib.mpv_set_option_string.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_char_p]
h = lib.mpv_create()
assert h, "mpv_create failed"
for k, v in [(b"vid", b"no"), (b"ao", b"null")]:
    assert lib.mpv_set_option_string(h, k, v) == 0
assert lib.mpv_initialize(h) == 0, "mpv_initialize failed"
EOF
    log "Load check passed: library loads and initializes an mpv instance."
}

# ------------------------------------------------------------------- bundle

build_libmpv() { # $1 = linux|mac|windows
    build_zlib
    build_freetype
    build_fribidi
    build_harfbuzz
    build_libass
    build_libplacebo
    build_ffmpeg
    build_mpv

    local dest="$OUT/libmpv"
    case "$1" in
        linux)
            cp "$PREFIX/lib/libmpv.so" "$dest"
            strip --strip-unneeded "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            log "Dynamic dependencies:"
            ldd "$dest"

            local unresolved
            unresolved="$(ldd -r "$dest" 2>&1 | grep -i "undefined symbol" || true)"
            if [ -n "$unresolved" ]; then
                echo "ERROR: unresolved symbols in $dest:"
                echo "$unresolved" | head -20
                exit 1
            fi

            local unexpected
            unexpected="$(ldd "$dest" | awk '{print $1}' | grep -Ev "$ALLOWED" || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: unexpected dynamic dependencies (not baseline system libs):"
                echo "$unexpected"
                exit 1
            fi
            log "Dependency check passed: only baseline system libraries required."

            run_load_check "$dest"
            ;;
        mac)
            cp "$PREFIX/lib/libmpv.dylib" "$dest"
            strip -x "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            log "Dynamic dependencies:"
            otool -L "$dest"

            # Everything under /usr/lib or /System/Library is part of macOS
            # itself. The first entry otool prints for a dylib is its own
            # install name — not a dependency — so it is filtered out.
            local unexpected
            unexpected="$(otool -L "$dest" | tail -n +2 | awk '{print $1}' \
                | grep -v "libmpv" \
                | grep -Ev '^(/usr/lib/|/System/Library/)' || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: unexpected dynamic dependencies (not macOS system libraries):"
                echo "$unexpected"
                exit 1
            fi
            log "Dependency check passed: only macOS system libraries required."

            run_load_check "$dest"
            ;;
        windows)
            cp "$PREFIX"/bin/libmpv-*.dll "$dest"
            "${CROSS_PREFIX}strip" --strip-unneeded "$dest"

            log "Bundled library: $dest ($(du -h "$dest" | cut -f1))"
            log "Imported DLLs:"
            "${CROSS_PREFIX}objdump" -p "$dest" | grep "DLL Name" || true

            # Windows system DLLs are always present; what must NOT appear are
            # mingw runtime or dependency DLLs — those mean static linking
            # silently failed.
            local unexpected
            unexpected="$("${CROSS_PREFIX}objdump" -p "$dest" | grep "DLL Name" \
                | grep -Ei "libwinpthread|libgcc|libstdc|libssp|zlib|libav|libass|libplacebo|libmpv" || true)"
            if [ -n "$unexpected" ]; then
                echo "ERROR: non-system DLL dependencies (static linking failed):"
                echo "$unexpected"
                exit 1
            fi
            log "Dependency check passed: only Windows system DLLs imported."
            ;;
        *)
            echo "ERROR: unknown mpv bundle target: $1"
            exit 1
            ;;
    esac
}
