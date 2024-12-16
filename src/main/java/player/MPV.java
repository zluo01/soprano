package player;

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

    class mpv_event extends Structure {
        public int event_id;
        public int error;
        public long reply_userdata;
        public Pointer data;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("event_id", "error", "reply_userdata", "data");
        }
    }
}
