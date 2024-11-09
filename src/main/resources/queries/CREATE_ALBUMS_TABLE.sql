create table if not exists albums
(
    name           TEXT    not null,
    date           TEXT,
    add_time       INTEGER not null,
    modified_time  INTEGER not null,
    total_duration INTEGER not null,
    id             INTEGER not null
        constraint albums_pk
            primary key
);
