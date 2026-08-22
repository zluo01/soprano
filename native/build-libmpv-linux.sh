#!/usr/bin/env bash
#
# Builds a self-contained, audio-only libmpv for bundling into the soprano jar
# (Linux, native build for the host architecture).
#
# FFmpeg, libass, libplacebo, freetype, harfbuzz, fribidi and zlib are compiled
# as static libraries and linked into a single libmpv shared library. The only
# runtime dependencies of the result are baseline system libraries that exist
# on any Linux install with working audio: glibc, libasound (ALSA) and the
# C++ runtime (libstdc++, pulled in by harfbuzz). ALSA is the sole audio
# output; PipeWire/PulseAudio systems are reached through their ALSA
# compatibility layer.
#
# The result inherits the build host's glibc baseline — build on a machine no
# newer (glibc-wise) than the deployment target.
#
# Output: native/out/resources/libmpv — the jar is built for ONE platform:
# run this script on (or for) the deployment target right before mvn package.

set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/common.sh"

CXX_RUNTIME_LIB=-lstdc++
MPV_OS_ARGS=(-Dalsa=enabled -Diconv=enabled)
# --exclude-libs keeps the statically linked FFmpeg/libass symbols private to
# libmpv.so so they cannot clash with other native libraries in the JVM.
MPV_LDFLAGS="-Wl,--exclude-libs,ALL"

require_tools "sudo dnf install gcc gcc-c++ make ninja-build pkgconf git curl python3 alsa-lib-devel
            or: sudo apt install build-essential ninja-build pkg-config git curl python3-venv libasound2-dev" \
    gcc g++ make ninja pkg-config git curl python3
if ! pkg-config --exists alsa; then
    echo "ERROR: ALSA headers not found (install alsa-lib-devel / libasound2-dev)."
    exit 1
fi

# mpv itself additionally needs the system pkg-config paths to find ALSA.
MPV_PC_LIBDIR="$PREFIX/lib/pkgconfig:$(pkg-config --variable pc_path pkg-config)"

init_dirs
setup_env

bootstrap_meson
if [ "$(uname -m)" = x86_64 ]; then
    build_nasm
fi

build_zlib
build_freetype
build_fribidi
build_harfbuzz
build_libass
build_libplacebo
build_ffmpeg
build_mpv
build_libwebp

# -------------------------------------------------------------------- output

DEST="$OUT/libmpv"
cp "$PREFIX/lib/libmpv.so" "$DEST"
strip --strip-unneeded "$DEST"

log "Bundled library: $DEST ($(du -h "$DEST" | cut -f1))"
log "Dynamic dependencies:"
ldd "$DEST"

UNRESOLVED="$(ldd -r "$DEST" 2>&1 | grep -i "undefined symbol" || true)"
if [ -n "$UNRESOLVED" ]; then
    echo "ERROR: unresolved symbols in $DEST:"
    echo "$UNRESOLVED" | head -20
    exit 1
fi

ALLOWED='linux-vdso|ld-linux|libc\.so|libm\.so|libpthread\.so|libdl\.so|librt\.so|libasound\.so|libstdc\+\+\.so|libgcc_s\.so'
UNEXPECTED="$(ldd "$DEST" | awk '{print $1}' | grep -Ev "$ALLOWED" || true)"
if [ -n "$UNEXPECTED" ]; then
    echo "ERROR: unexpected dynamic dependencies (not baseline system libs):"
    echo "$UNEXPECTED"
    exit 1
fi
log "Dependency check passed: only baseline system libraries required."

run_load_check "$DEST"

# ---------------------------------------------------- libwebp bundle output

WEBP_DEST="$OUT/libwebp"
gcc -shared -o "$WEBP_DEST" \
    -Wl,--whole-archive "$PREFIX/lib/libwebp.a" "$PREFIX/lib/libsharpyuv.a" -Wl,--no-whole-archive \
    -lm -lpthread
strip --strip-unneeded "$WEBP_DEST"

log "Bundled library: $WEBP_DEST ($(du -h "$WEBP_DEST" | cut -f1))"
WEBP_UNEXPECTED="$(ldd "$WEBP_DEST" | awk '{print $1}' | grep -Ev "$ALLOWED" || true)"
if [ -n "$WEBP_UNEXPECTED" ]; then
    echo "ERROR: unexpected dynamic dependencies in libwebp bundle:"
    echo "$WEBP_UNEXPECTED"
    exit 1
fi
run_webp_load_check "$WEBP_DEST"
