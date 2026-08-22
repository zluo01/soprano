# Shared machinery for the build-libmpv-<os>.sh scripts. Not runnable on its
# own — each entry script sets the platform knobs, then calls the stage
# functions below in order.
#
# Platform knobs read by the stages (set before calling them):
#   CXX_RUNTIME_LIB    C++ runtime harfbuzz needs (-lstdc++, or -lc++ on mac)
#   MPV_OS_ARGS        audio output etc. meson args for the mpv stage; a bash
#                      array, so one arg may contain spaces (mac swift-flags)
#   MPV_LDFLAGS        extra link flags for the mpv stage (optional)
#   MPV_PC_LIBDIR      pkg-config path for the mpv stage (optional; defaults
#                      to the isolated prefix)
#   MESON_CROSS_ARGS   --cross-file argument for cross builds (optional)
#   LIBASS_EXTRA_CONF  extra ./configure args for libass (optional)
#   FFMPEG_TARGET_ARGS cross/target ./configure args for ffmpeg (optional)
#   CROSS_PREFIX       toolchain prefix for cross builds (optional)
#
# NOTE: the repository path must not contain spaces (ffmpeg's configure and
# several upstream build systems cannot cope with them).

NATIVE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$NATIVE_DIR/work"
TOOLS="$WORK/tools"
PREFIX="$WORK/prefix"
SRC="$WORK/src"
OUT="$NATIVE_DIR/out/resources"
JOBS="$(nproc 2>/dev/null || sysctl -n hw.ncpu)"

. "$NATIVE_DIR/versions.sh"

# freetype tags releases as VER-2-14-3; derive the dotted form used in URLs.
FREETYPE_VERSION="$(echo "${FREETYPE_TAG#VER-}" | tr '-' '.')"

# Stamps are tied to the content of versions.sh: bumping any pin invalidates
# them all, so a rerun rebuilds everything against the new set instead of
# silently reusing stages built from old versions.
STAMPS="$WORK/stamps-$(cksum "$NATIVE_DIR/versions.sh" | awk '{print $1}')"

log() { echo ">>> $*"; }
done_stamp() { [ -f "$STAMPS/$1.done" ]; }
mark_done() { touch "$STAMPS/$1.done"; }

init_dirs() {
    # A fresh stamp set (new version pins) starts from a clean prefix so no
    # artifacts from a previous version set can leak into the new build.
    if [ ! -d "$STAMPS" ]; then
        rm -rf "$PREFIX"
        rm -rf "$WORK"/stamps-* "$WORK"/stamps
    fi
    mkdir -p "$WORK" "$TOOLS" "$PREFIX/lib/pkgconfig" "$PREFIX/include" \
             "$SRC" "$STAMPS" "$OUT"
}

# Fail fast with one clear message instead of a mid-build mystery.
# Usage: require_tools "<install hint>" tool...
require_tools() {
    local hint="$1" missing="" t
    shift
    for t in "$@"; do
        command -v "$t" >/dev/null 2>&1 || missing="$missing $t"
    done
    if ! python3 -c "import ensurepip" >/dev/null 2>&1; then
        missing="$missing python3-venv"
    fi
    if [ -n "$missing" ]; then
        echo "ERROR: missing required tools:$missing"
        echo "Install them with: $hint"
        exit 1
    fi
}

sha256_of() {
    (sha256sum "$1" 2>/dev/null || shasum -a 256 "$1") | awk '{print $1}'
}

fetch() { # fetch <url> <expected sha256>
    local url="$1" expected="$2" tarball actual
    tarball="$SRC/$(basename "$url")"
    if [ ! -f "$tarball" ]; then
        log "Downloading $url"
        curl -fL --retry 3 -o "$tarball.tmp" "$url"
        mv "$tarball.tmp" "$tarball"
    fi
    actual="$(sha256_of "$tarball")"
    if [ "$actual" != "$expected" ]; then
        echo "ERROR: checksum mismatch for $(basename "$tarball")"
        echo "  expected: $expected"
        echo "  actual:   $actual"
        echo "After a version bump, verify the new tarball and update the *_SHA256 pin in versions.sh."
        exit 1
    fi
    tar -xf "$tarball" -C "$SRC"
}

setup_env() {
    export PATH="$TOOLS/venv/bin:$TOOLS/bin:$PATH"
    export CFLAGS="-O2 -pipe -fPIC"
    export CXXFLAGS="-O2 -pipe -fPIC"
    # While building the dependencies, only our own prefix is visible to
    # pkg-config. This prevents any component from silently picking up
    # optional system libraries (e.g. freetype finding bzip2, libass finding
    # libunibreak) which would leak dynamic dependencies into the final
    # library. Stages that legitimately need a system package widen the path
    # themselves (see MPV_PC_LIBDIR).
    export PKG_CONFIG_LIBDIR="$PREFIX/lib/pkgconfig"
}

# ------------------------------------------------------------- build tools

bootstrap_meson() { # args: extra pip packages (e.g. ninja on macOS)
    if ! done_stamp meson; then
        log "Bootstrapping meson $MESON_VERSION into a private venv"
        python3 -m venv "$TOOLS/venv"
        "$TOOLS/venv/bin/pip" -q install "meson==$MESON_VERSION" jinja2 "$@"
        mark_done meson
    fi
}

# Only needed when the TARGET is x86: nasm assembles ffmpeg's (and libass's)
# x86 SIMD sources. ARM assembly goes through the C compiler's assembler.
build_nasm() {
    if ! done_stamp nasm; then
        fetch "https://www.nasm.us/pub/nasm/releasebuilds/$NASM_VERSION/nasm-$NASM_VERSION.tar.xz" "$NASM_SHA256"
        log "Building nasm"
        ( cd "$SRC/nasm-$NASM_VERSION" \
          && ./configure --prefix="$TOOLS" >/dev/null \
          && make -j"$JOBS" >/dev/null && make install >/dev/null )
        mark_done nasm
    fi
}

# macOS ships no pkg-config; bootstrap pkgconf into the tools prefix.
build_pkgconf() {
    if ! done_stamp pkgconf; then
        fetch "https://distfiles.ariadne.space/pkgconf/pkgconf-$PKGCONF_VERSION.tar.xz" "$PKGCONF_SHA256"
        log "Building pkgconf"
        ( cd "$SRC/pkgconf-$PKGCONF_VERSION" \
          && ./configure --prefix="$TOOLS" >/dev/null \
          && make -j"$JOBS" >/dev/null && make install >/dev/null )
        ln -sf pkgconf "$TOOLS/bin/pkg-config"
        mark_done pkgconf
    fi
}

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

# Static libwebp for the bundled WebP encoder (cover art optimization).
# Only the encoding core is needed; every optional feature and tool is off.
# The per-OS scripts link libwebp.a + libsharpyuv.a into one shared library.
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

# dlopen the built libwebp bundle and encode a tiny image — verifies the
# encoder works end to end. (Host-run platforms only.)
run_webp_load_check() { # $1 = library path
    python3 - "$1" <<'EOF'
import ctypes, sys
lib = ctypes.CDLL(sys.argv[1])
lib.WebPGetEncoderVersion.restype = ctypes.c_int
v = lib.WebPGetEncoderVersion()
print(f"libwebp encoder version {v >> 16}.{(v >> 8) & 0xFF}.{v & 0xFF}")
lib.WebPEncodeRGBA.restype = ctypes.c_size_t
lib.WebPEncodeRGBA.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.c_int,
                               ctypes.c_int, ctypes.c_float, ctypes.POINTER(ctypes.c_void_p)]
out = ctypes.c_void_p()
size = lib.WebPEncodeRGBA(b"\xff\x00\x00\xff" * 4, 2, 2, 8, 75.0, ctypes.byref(out))
assert size > 0, "WebPEncodeRGBA failed"
lib.WebPFree(out)
EOF
    log "libwebp load check passed: encoder produces output."
}

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
