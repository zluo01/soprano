create index if not exists albums_name_add_time_index
    on albums (name asc, add_time desc);
