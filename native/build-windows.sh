#!/usr/bin/env bash
#
# Builds a self-contained, audio-only libmpv for bundling into the soprano jar
# (Windows x86-64, cross-compiled FROM LINUX with the mingw-w64 toolchain —
# this is mpv's officially supported way to produce Windows binaries).
#
# Same construction as the Linux build: all libraries statically linked, plus
# a fully static mingw runtime (-static bakes in winpthreads/libgcc/libstdc++)
# so the DLL depends only on Windows system DLLs. WASAPI is the sole audio
# output — part of Windows itself, no external libraries.
#
# Host requirements (Fedora):
#   sudo dnf install mingw64-gcc mingw64-gcc-c++
# (Debian/Ubuntu: sudo apt install gcc-mingw-w64-x86-64 g++-mingw-w64-x86-64)
# plus the same base tools as the Linux build.
#
# STATUS: written to mirror the verified Linux build but not yet exercised —
# expect to iterate on the first run. The produced DLL must be verified on a
# Windows machine (run the jar); run_load_check cannot execute it on Linux.
#
# Output: native/out/resources/libmpv — the jar is built for ONE platform:
# this overwrites any previously built library, so package the Windows jar
# right after running this script.

set -euo pipefail

. "$(dirname "${BASH_SOURCE[0]}")/common.sh"

CXX_RUNTIME_LIB=-lstdc++
CROSS_PREFIX=x86_64-w64-mingw32-
# WASAPI only; mingw has no iconv. -static folds the mingw runtime into the
# DLL so no libwinpthread/libgcc/libstdc++ DLLs are needed at runtime.
# win32-threads must be enabled explicitly (auto_features turns it off, and
# mpv's win32 timer code conflicts with the pthreads fallback).
MPV_OS_ARGS=(-Dwasapi=enabled -Diconv=disabled -Dwin32-threads=enabled)
MPV_LDFLAGS="-static"
FFMPEG_TARGET_ARGS="--enable-cross-compile --target-os=mingw32 --arch=x86_64 --cross-prefix=$CROSS_PREFIX"
LIBASS_EXTRA_CONF="--host=x86_64-w64-mingw32 --disable-directwrite"
LIBWEBP_EXTRA_CONF="--host=x86_64-w64-mingw32"

require_tools "sudo dnf install mingw64-gcc mingw64-gcc-c++ make ninja-build pkgconf git curl python3" \
    "${CROSS_PREFIX}gcc" "${CROSS_PREFIX}g++" gcc make ninja pkg-config git curl python3

init_dirs
setup_env

# Autotools prefers a host- prefixedx86_64-w64-mingw32-pkg-config when one
# exists (Fedora ships one that rewrites our prefix paths into the mingw
# sysroot). Pin the plain pkg-config; PKG_CONFIG_LIBDIR already isolates it.
export PKG_CONFIG=pkg-config

CROSS_FILE="$WORK/mingw64-cross.ini"
cat > "$CROSS_FILE" <<EOF
[binaries]
c = '${CROSS_PREFIX}gcc'
cpp = '${CROSS_PREFIX}g++'
ar = '${CROSS_PREFIX}ar'
strip = '${CROSS_PREFIX}strip'
windres = '${CROSS_PREFIX}windres'
dlltool = '${CROSS_PREFIX}dlltool'
pkg-config = 'pkg-config'

[host_machine]
system = 'windows'
cpu_family = 'x86_64'
cpu = 'x86_64'
endian = 'little'
EOF
MESON_CROSS_ARGS="--cross-file $CROSS_FILE"

bootstrap_meson
build_nasm

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
cp "$PREFIX"/bin/libmpv-*.dll "$DEST"
"${CROSS_PREFIX}strip" --strip-unneeded "$DEST"

log "Bundled library: $DEST ($(du -h "$DEST" | cut -f1))"
log "Imported DLLs:"
"${CROSS_PREFIX}objdump" -p "$DEST" | grep "DLL Name" || true

# Windows system DLLs are always present; what must NOT appear are mingw
# runtime or dependency DLLs — those mean static linking silently failed.
UNEXPECTED="$("${CROSS_PREFIX}objdump" -p "$DEST" | grep "DLL Name" \
    | grep -Ei "libwinpthread|libgcc|libstdc|libssp|zlib|libav|libass|libplacebo|libmpv" || true)"
if [ -n "$UNEXPECTED" ]; then
    echo "ERROR: non-system DLL dependencies (static linking failed):"
    echo "$UNEXPECTED"
    exit 1
fi
log "Dependency check passed: only Windows system DLLs imported."

# ---------------------------------------------------- libwebp bundle output

WEBP_DEST="$OUT/libwebp"
# mingw appends .exe to extensionless output names; link with a .dll name
# and rename to the fixed resource name afterwards.
"${CROSS_PREFIX}gcc" -shared -static -o "$WORK/libwebp.dll" \
    -Wl,--whole-archive "$PREFIX/lib/libwebp.a" "$PREFIX/lib/libsharpyuv.a" -Wl,--no-whole-archive
"${CROSS_PREFIX}strip" --strip-unneeded "$WORK/libwebp.dll"
mv "$WORK/libwebp.dll" "$WEBP_DEST"

log "Bundled library: $WEBP_DEST ($(du -h "$WEBP_DEST" | cut -f1))"
WEBP_UNEXPECTED="$("${CROSS_PREFIX}objdump" -p "$WEBP_DEST" | grep "DLL Name" \
    | grep -Ei "libwinpthread|libgcc|libstdc|libssp" || true)"
if [ -n "$WEBP_UNEXPECTED" ]; then
    echo "ERROR: non-system DLL dependencies in libwebp bundle:"
    echo "$WEBP_UNEXPECTED"
    exit 1
fi
log "NOTE: run the jar on a Windows machine to fully verify these libraries."
