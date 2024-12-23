SELECT id,
       name,
       COUNT(album) AS albumCount
FROM albumArtists
WHERE name LIKE ?
GROUP BY id, name
ORDER BY name;
