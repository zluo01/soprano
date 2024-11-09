create table if not exists artists
(
    album integer not null
        constraint artists_albums_id_fk
            references albums,
    id    integer not null,
    name  TEXT    not null,
    constraint artists_pk
        unique (id, album)
);

