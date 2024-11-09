INSERT INTO songs(name, artists, album, path, date, genre, composer, performer, disc, track_num, duration,
                  modified_time, add_time)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
ON CONFLICT (path) DO NOTHING
