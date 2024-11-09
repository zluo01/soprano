INSERT INTO artists(id, name, album)
VALUES (?, ?, ?)
ON CONFLICT (id, album) DO NOTHING
