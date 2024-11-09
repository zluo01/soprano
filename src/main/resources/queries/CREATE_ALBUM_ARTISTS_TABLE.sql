create table if not exists albumArtists
(
    id    INTEGER not null,
    name  TEXT    not null,
    album INTEGER not null
        constraint artists_albums_id_fk
            references albums,
    constraint albumArtists_pk
        unique (id, album)
);

