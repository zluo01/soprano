package player.mpv;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.List;

public interface MPV extends Library {
    long mpv_client_api_version();

    long mpv_create();

    int mpv_initialize(long handle);

    int mpv_command(long handle, String[] args);

    int mpv_command_string(long handle, String args);

    Pointer mpv_get_property(long handle, String name, int format, Pointer data);

    Pointer mpv_get_property_string(long handle, String name);

    int mpv_set_property_string(long handle, String name, String data);

    int mpv_set_option(long handle, String name, int format, Pointer data);

    int mpv_set_option_string(long handle, String name, String data);

    void mpv_free(Pointer data);

    mpv_event mpv_wait_event(long handle, double timeOut);

    int mpv_request_event(long handle, int event_id, int enable);

    // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1581-L1626
    class mpv_event extends Structure {
        /**
         * One of mpv_event. Keep in mind that later ABI compatible releases might
         * add new event types. These should be ignored by the API user.
         */
        public int event_id;
        /**
         * This is mainly used for events that are replies to (asynchronous)
         * requests. It contains a status code, which is >= 0 on success, or < 0
         * on error (a mpv_error value). Usually, this will be set if an
         * asynchronous request fails.
         * Used for:
         * MPV_EVENT_GET_PROPERTY_REPLY
         * MPV_EVENT_SET_PROPERTY_REPLY
         * MPV_EVENT_COMMAND_REPLY
         */
        public int error;
        /**
         * If the event is in reply to a request (made with this API and this
         * API handle), this is set to the reply_userdata parameter of the request
         * call. Otherwise, this field is 0.
         * Used for:
         * MPV_EVENT_GET_PROPERTY_REPLY
         * MPV_EVENT_SET_PROPERTY_REPLY
         * MPV_EVENT_COMMAND_REPLY
         * MPV_EVENT_PROPERTY_CHANGE
         * MPV_EVENT_HOOK
         */
        public long reply_userdata;
        /**
         * The meaning and contents of the data member depend on the event_id:
         * MPV_EVENT_GET_PROPERTY_REPLY:     mpv_event_property*
         * MPV_EVENT_PROPERTY_CHANGE:        mpv_event_property*
         * MPV_EVENT_LOG_MESSAGE:            mpv_event_log_message*
         * MPV_EVENT_CLIENT_MESSAGE:         mpv_event_client_message*
         * MPV_EVENT_START_FILE:             mpv_event_start_file* (since v1.108)
         * MPV_EVENT_END_FILE:               mpv_event_end_file*
         * MPV_EVENT_HOOK:                   mpv_event_hook*
         * MPV_EVENT_COMMAND_REPLY*          mpv_event_command*
         * other: NULL
         * <p>
         * Note: future enhancements might add new event structs for existing or new
         * event types.
         */
        public Pointer data;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("event_id", "error", "reply_userdata", "data");
        }
    }

    // https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h#L1504-L1546
    class mpv_event_end_file extends Structure {
        /**
         * Corresponds to the values in enum mpv_end_file_reason.
         * <p>
         * Unknown values should be treated as unknown.
         */
        public int reason;
        /**
         * If reason==MPV_END_FILE_REASON_ERROR, this contains a mpv error code
         * (one of MPV_ERROR_...) giving an approximate reason why playback
         * failed. In other cases, this field is 0 (no error).
         * Since API version 1.9.
         */
        public int error;
        /**
         * Playlist entry ID of the file that was being played or attempted to be
         * played. This has the same value as the playlist_entry_id field in the
         * corresponding mpv_event_start_file event.
         * Since API version 1.108.
         */
        public long playlist_entry_id;
        /**
         * If loading ended, because the playlist entry to be played was for example
         * a playlist, and the current playlist entry is replaced with a number of
         * other entries. This may happen at least with MPV_END_FILE_REASON_REDIRECT
         * (other event types may use this for similar but different purposes in the
         * future). In this case, playlist_insert_id will be set to the playlist
         * entry ID of the first inserted entry, and playlist_insert_num_entries to
         * the total number of inserted playlist entries. Note this in this specific
         * case, the ID of the last inserted entry is playlist_insert_id+num-1.
         * Beware that depending on circumstances, you may observe the new playlist
         * entries before seeing the event (e.g. reading the "playlist" property or
         * getting a property change notification before receiving the event).
         * Since API version 1.108.
         */
        public long playlist_insert_id;
        /**
         * See playlist_insert_id. Only non-0 if playlist_insert_id is valid. Never
         * negative.
         * Since API version 1.108.
         */
        public int playlist_insert_num_entries;


        public mpv_event_end_file() {
            super();
        }

        public mpv_event_end_file(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("reason",
                           "error",
                           "playlist_entry_id",
                           "playlist_insert_id",
                           "playlist_insert_num_entries");
        }
    }
}
