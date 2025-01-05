create table if not exists genres
(
    name  TEXT    not null,
    album INTEGER not null
        constraint genres_albums_id_fk
            references albums
            on delete cascade,
    id    INTEGER not null,
    constraint genres_pk
        unique (id, album)
);

