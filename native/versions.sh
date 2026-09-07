# Pinned versions and tarball checksums for the bundled libmpv build, shared
# by every build-<os>.sh script. Bump here, then rerun the script(s) —
# stamps are fingerprinted to this file, so any change triggers a clean
# rebuild.
#
# The set must stay mutually compatible: mpv's release drives the minimums
# (check meson.build of the mpv tag for libplacebo/FFmpeg requirements).
#
# The "# renovate:" annotations let Renovate open PRs when upstream releases;
# a version bump then requires updating the matching *_SHA256 (the build
# fails with both sums printed — verify the new tarball before copying).

# renovate: datasource=github-tags depName=netwide-assembler/nasm versioning=loose extractVersion=^nasm-(?<version>.+)$
NASM_VERSION=3.02
NASM_SHA256=87336eba53b4acfe917424ab5d500d2b0054d9f5148d35c2273ccf2cfb712f0d

# renovate: datasource=github-tags depName=pkgconf/pkgconf extractVersion=^pkgconf-(?<version>.+)$
PKGCONF_VERSION=2.5.1
PKGCONF_SHA256=3a9080ac51d03615e7c1910a0a2a8df08424892b5f13b0628a204d3fcce0ea8b

# renovate: datasource=github-releases depName=madler/zlib extractVersion=^v(?<version>.+)$
ZLIB_VERSION=1.3.2
ZLIB_SHA256=d7a0654783a4da529d1bb793b7ad9c3318020af77667bcae35f95d0e42a792f3

# renovate: datasource=github-tags depName=freetype/freetype versioning=regex:^VER-(?<major>\d+)-(?<minor>\d+)-(?<patch>\d+)$
FREETYPE_TAG=VER-2-14-3
FREETYPE_SHA256=36bc4f1cc413335368ee656c42afca65c5a3987e8768cc28cf11ba775e785a5f

# renovate: datasource=github-releases depName=fribidi/fribidi extractVersion=^v(?<version>.+)$
FRIBIDI_VERSION=1.0.16
FRIBIDI_SHA256=1b1cde5b235d40479e91be2f0e88a309e3214c8ab470ec8a2744d82a5a9ea05c

# renovate: datasource=github-releases depName=harfbuzz/harfbuzz
HARFBUZZ_VERSION=14.4.0
HARFBUZZ_SHA256=9dae9538aae2ffdf70cec31f2c27bf68e2aaeeae3112688467697d5faf6194f7

# renovate: datasource=github-releases depName=libass/libass
LIBASS_VERSION=0.17.5
LIBASS_SHA256=2dca25c0e0c837ddf00b52011b3f82cac1e4ddd3ad018227806b0c2288864acc

# renovate: datasource=github-tags depName=haasn/libplacebo extractVersion=^v(?<version>.+)$
LIBPLACEBO_VERSION=7.360.1

# renovate: datasource=github-tags depName=FFmpeg/FFmpeg versioning=semver-coerced extractVersion=^n(?<version>.+)$
FFMPEG_VERSION=8.1.2
FFMPEG_SHA256=464beb5e7bf0c311e68b45ae2f04e9cc2af88851abb4082231742a74d97b524c

# renovate: datasource=github-releases depName=mpv-player/mpv extractVersion=^v(?<version>.+)$
MPV_VERSION=0.41.0
MPV_SHA256=ee21092a5ee427353392360929dc64645c54479aefdb5babc5cfbb5fad626209

# renovate: datasource=github-tags depName=webmproject/libwebp extractVersion=^v(?<version>.+)$
LIBWEBP_VERSION=1.6.0
LIBWEBP_SHA256=e4ab7009bf0629fd11982d4c2aa83964cf244cffba7347ecd39019a9e38c4564

# renovate: datasource=pypi depName=meson
MESON_VERSION=1.12.0
