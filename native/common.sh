# Shared machinery for the build-<os>.sh scripts: directories, version pins,
# stamps, fetching, environment, and build-tool bootstraps. Not runnable on
# its own. The bundles themselves live in their own packages — mpv/build.sh
# (build_libmpv) and image/build.sh (build_libimage) — each sourced by the
# entry scripts after this file.
#
# Platform knobs read by the mpv stages (set before calling them):
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

# NOTE: the work directory holds the build state of ONE target. When
# switching targets locally (e.g. linux → windows cross build), remove
# native/work first; each CI runner is fresh so this never applies there.

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
    # A fresh stamp set (new version pins) starts from clean sources and a
    # clean prefix so nothing from a previous version set can leak in.
    if [ ! -d "$STAMPS" ]; then
        rm -rf "$PREFIX" "$SRC"
        rm -rf "$WORK"/stamps-*
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
