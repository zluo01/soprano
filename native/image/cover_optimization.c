/*
 * Album cover art optimization pipeline. stb_image + stb_image_resize2 for decoding and resizing, libwebp for encoding to webp outputs
 *
 * stb_image / stb_image_resize2 are vendored headers (https://github.com/nothings/stb);
 * libwebp is linked statically from the native build prefix.
 */
#define STB_IMAGE_IMPLEMENTATION
#define STBI_FAILURE_USERMSG
#include "stb_image.h"
#define STB_IMAGE_RESIZE_IMPLEMENTATION
#include "stb_image_resize2.h"
#include <webp/encode.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

static int fit(int size, int a, int b) {
    int v = (int) floorf(size * (float) a / b + 0.5f);
    return v < 1 ? 1 : v;
}

/*
 * do aspect-fit scale with transparent padding. then encode to webp format.
 */
static unsigned char* encode_one(const unsigned char* src, int w, int h,
                                 int size, float quality, size_t* out_len) {
    int tw, th;
    if (w > h) { tw = size; th = fit(size, h, w); }
    else       { th = size; tw = fit(size, w, h); }

    unsigned char* scaled = malloc((size_t) tw * th * 4);
    unsigned char* canvas = calloc((size_t) size * size, 4);
    unsigned char* webp = NULL;

    if (scaled && canvas && stbir_resize_uint8_linear(src, w, h, 0, scaled, tw, th, 0, STBIR_RGBA)) {
        const int off_x = (size - tw) / 2;
        const int off_y = (size - th) / 2;
        for (int row = 0; row < th; row++) {
            memcpy(canvas + (((size_t) (off_y + row) * size + off_x) * 4),
                   scaled + ((size_t) row * tw * 4), (size_t) tw * 4);
        }
        size_t n = WebPEncodeRGBA(canvas, size, size, size * 4, quality, &webp);
        if (n == 0) webp = NULL; else *out_len = n;
    }
    free(scaled);
    free(canvas);
    return webp;
}

/*
 * Decode image data once, then do resize and padding for each dimensions
 */
int cover_optimize(const unsigned char* data, int len,
                   const int* dims, int dim_count, float quality,
                   unsigned char** out_bufs, size_t* out_lens) {
    int w, h, comp;
    unsigned char* src = stbi_load_from_memory(data, len, &w, &h, &comp, 4);
    if (!src) return 0;

    int produced;
    for (produced = 0; produced < dim_count; produced++) {
        out_bufs[produced] = encode_one(src, w, h, dims[produced], quality, &out_lens[produced]);
        if (!out_bufs[produced]) break;
    }
    stbi_image_free(src);

    if (produced == dim_count) return 1;
    while (produced-- > 0) WebPFree(out_bufs[produced]);
    return 0;
}

void cover_free(unsigned char* p) {
    WebPFree(p);
}
