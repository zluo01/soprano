create table if not exists artists
(
    album INTEGER not null
        constraint artists_albums_id_fk
            references albums,
    id    INTEGER not null,
    name  TEXT    not null,
    constraint artists_pk
        unique (id, album)
);

