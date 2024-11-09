INSERT INTO genres(id, name, album)
VALUES (?, ?, ?)
ON CONFLICT (id,album) DO NOTHING
