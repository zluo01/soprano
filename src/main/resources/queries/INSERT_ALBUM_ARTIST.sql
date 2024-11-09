INSERT INTO albumArtists(id, name, album)
VALUES (?, ?, ?)
ON CONFLICT (id, album) DO NOTHING
