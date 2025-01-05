DELETE
FROM albums
WHERE albums.id not in (SELECT songs.album from songs)
