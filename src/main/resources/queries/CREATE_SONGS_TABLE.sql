create table if not exists songs
(
    name          TEXT    not null,
    artists       TEXT,
    album         INTEGER not null
        constraint songs_albums_id_fk
            references albums
            on delete cascade,
    path          TEXT    not null
        constraint songs_pk
            unique,
    genre         TEXT,
    date          TEXT,
    composer      TEXT,
    performer     TEXT,
    disc          INTEGER not null,
    track_num     INTEGER not null,
    duration      INTEGER not null,
    modified_time INTEGER not null,
    add_time      INTEGER not null
);

