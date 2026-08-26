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
# Output: native/out/resources/libmpv.gz — the jar is built for ONE platform:
# run this script on (or for) the deployment target right before mvn package.

set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/common.sh"
. "$NATIVE_DIR/mpv/build.sh"
. "$NATIVE_DIR/image/build.sh"

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

# Baseline system libraries allowed as dynamic dependencies of the bundles.
ALLOWED='linux-vdso|ld-linux|libc\.so|libm\.so|libpthread\.so|libdl\.so|librt\.so|libasound\.so|libstdc\+\+\.so|libgcc_s\.so'

init_dirs
setup_env

bootstrap_meson
if [ "$(uname -m)" = x86_64 ]; then
    build_nasm
fi

# ------------------------------------------------------------------ bundles

build_libmpv linux
build_libimage linux
