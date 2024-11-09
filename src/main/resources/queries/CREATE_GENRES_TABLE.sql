create table if not exists genres
(
    name  TEXT    not null,
    album INTEGER not null
        constraint genres_albums_id_fk
            references albums,
    id    integer not null,
    constraint genres_pk
        unique (id, album)
);

