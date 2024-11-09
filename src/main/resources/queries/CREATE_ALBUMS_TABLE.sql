create table if not exists albums
(
    name           TEXT    not null,
    date           TEXT,
    add_time       integer not null,
    modified_time  integer not null,
    total_duration integer not null,
    id             integer not null
        constraint albums_pk
            primary key
);
