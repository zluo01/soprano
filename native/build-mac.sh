#!/usr/bin/env bash
#
# Builds a self-contained, audio-only libmpv for bundling into the soprano jar
# (macOS, native build for the host architecture — run once on an Apple
# Silicon Mac and once on an Intel Mac for both slices).
#
# Same construction as the Linux build: all libraries statically linked into
# one libmpv.dylib. CoreAudio is the sole audio output — a system framework,
# present on every Mac, so the result has no third-party runtime dependencies.
#
# Host requirements: Xcode Command Line Tools (clang, make, git) and python3.
# meson, ninja, pkgconf and (on Intel) nasm are bootstrapped automatically —
# no Homebrew needed.
#
# NOTE: libwebp's shipped configure prints two harmless errors on macOS
# ("-a: command not found" from a broken line continuation in its AVX2 guard,
# and a GNU-only "sed -i" call whose substitution only matters for Windows
# DLL builds). Both are upstream bugs with no effect on the produced library.
#
# Output: native/out/resources/libmpv.gz — the jar is built for ONE platform:
# run this script on the deployment target's architecture before mvn package.

set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/common.sh"
. "$NATIVE_DIR/mpv/build.sh"
. "$NATIVE_DIR/image/build.sh"

CXX_RUNTIME_LIB=-lc++

# Compatibility floor, not a version choice: 11.0 covers every arm64 Mac,
# and the build host's newer SDK does not raise it.
export MACOSX_DEPLOYMENT_TARGET=11.0

# coreaudio needs cocoa (undefined cfstr_* symbols without it), and cocoa
# needs the swift bridge. iconv: not needed for audio-only, and meson can't
# resolve macOS's libiconv. swiftc ignores MACOSX_DEPLOYMENT_TARGET, hence
# the explicit -target.
MPV_OS_ARGS=(
    -Dcoreaudio=enabled -Dcocoa=enabled -Dswift-build=enabled
    -Diconv=disabled
    "-Dswift-flags=-target $(uname -m)-apple-macos$MACOSX_DEPLOYMENT_TARGET"
)
LIBASS_EXTRA_CONF="--disable-coretext"

require_tools "xcode-select --install   (Xcode Command Line Tools)" \
    cc c++ make git curl python3

init_dirs
setup_env

bootstrap_meson ninja
build_pkgconf
if [ "$(uname -m)" = x86_64 ]; then
    build_nasm
fi

# ------------------------------------------------------------------ bundles

build_libmpv mac
build_libimage mac
