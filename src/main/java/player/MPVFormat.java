package player;

/**
 * <a href="https://github.com/mpv-player/mpv/blob/master/libmpv/client.h#L630">MPV format</a>
 */
public final class MPVFormat {
    private MPVFormat() {
    }

    /**
     * Invalid. Sometimes used for empty values. This is always defined to 0,
     * so a normal 0-init of mpv_format (or e.g. mpv_node) is guaranteed to set
     * this it to MPV_FORMAT_NONE (which makes some things saner as consequence).
     */
    public static final int MPV_FORMAT_NONE = 0;
    /**
     * The basic type is char*. It returns the raw property string, like
     * using ${=property} in input.conf (see input.rst).
     * <p>
     * NULL isn't an allowed value.
     * <p>
     * Warning: although the encoding is usually UTF-8, this is not always the
     * case. File tags often store strings in some legacy codepage,
     * and even filenames don't necessarily have to be in UTF-8 (at
     * least on Linux). If you pass the strings to code that requires
     * valid UTF-8, you have to sanitize it in some way.
     * On Windows, filenames are always UTF-8, and libmpv converts
     * between UTF-8 and UTF-16 when using win32 API functions. See
     * the "Encoding of filenames" section for details.
     * <p>
     * Example for reading:
     * <p>
     * char *result = NULL;
     * if (mpv_get_property(ctx, "property", MPV_FORMAT_STRING, &result) < 0)
     * goto error;
     * printf("%s\n", result);
     * mpv_free(result);
     * <p>
     * Or just use mpv_get_property_string().
     * <p>
     * Example for writing:
     * <p>
     * char *value = "the new value";
     * // yep, you pass the address to the variable
     * // (needed for symmetry with other types and mpv_get_property)
     * mpv_set_property(ctx, "property", MPV_FORMAT_STRING, &value);
     * <p>
     * Or just use mpv_set_property_string().
     */
    public static final int MPV_FORMAT_STRING = 1;
    /**
     * The basic type is char*. It returns the OSD property string, like
     * using ${property} in input.conf (see input.rst). In many cases, this
     * is the same as the raw string, but in other cases it's formatted for
     * display on OSD. It's intended to be human-readable. Do not attempt to
     * parse these strings.
     * <p>
     * Only valid when doing read access. The rest works like MPV_FORMAT_STRING.
     */
    public static final int MPV_FORMAT_OSD_STRING = 2;
    /**
     * The basic type is int. The only allowed values are 0 ("no")
     * and 1 ("yes").
     * <p>
     * Example for reading:
     * <p>
     * int result;
     * if (mpv_get_property(ctx, "property", MPV_FORMAT_FLAG, &result) < 0)
     * goto error;
     * printf("%s\n", result ? "true" : "false");
     * <p>
     * Example for writing:
     * <p>
     * int flag = 1;
     * mpv_set_property(ctx, "property", MPV_FORMAT_FLAG, &flag);
     */
    public static final int MPV_FORMAT_FLAG = 3;
    /**
     * The basic type is int64_t.
     */
    public static final int MPV_FORMAT_INT64 = 4;
    /**
     * The basic type is double.
     */
    public static final int MPV_FORMAT_DOUBLE = 5;
    /**
     * The type is mpv_node.
     * <p>
     * For reading, you usually would pass a pointer to a stack-allocated
     * mpv_node value to mpv, and when you're done you call
     * mpv_free_node_contents(&node).
     * You're expected not to write to the data - if you have to, copy it
     * first (which you have to do manually).
     * <p>
     * For writing, you construct your own mpv_node, and pass a pointer to the
     * API. The API will never write to your data (and copy it if needed), so
     * you're free to use any form of allocation or memory management you like.
     * <p>
     * Warning: when reading, always check the mpv_node.format member. For
     * example, properties might change their type in future versions
     * of mpv, or sometimes even during runtime.
     * <p>
     * Example for reading:
     * <p>
     * mpv_node result;
     * if (mpv_get_property(ctx, "property", MPV_FORMAT_NODE, &result) < 0)
     * goto error;
     * printf("format=%d\n", (int)result.format);
     * mpv_free_node_contents(&result).
     * <p>
     * Example for writing:
     * <p>
     * mpv_node value;
     * value.format = MPV_FORMAT_STRING;
     * value.u.string = "hello";
     * mpv_set_property(ctx, "property", MPV_FORMAT_NODE, &value);
     */
    public static final int MPV_FORMAT_NODE = 6;
    /**
     * Used with mpv_node only. Can usually not be used directly.
     */
    public static final int MPV_FORMAT_NODE_ARRAY = 7;
    /**
     * See MPV_FORMAT_NODE_ARRAY.
     */
    public static final int MPV_FORMAT_NODE_MAP = 8;
    /**
     * A raw, untyped byte array. Only used with mpv_node, and only in
     * some very specific situations. (Some commands use it.)
     */
    public static final int MPV_FORMAT_BYTE_ARRAY = 9;
}
