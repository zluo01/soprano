SELECT a.id,
       a.name,
       albumArtists.name AS artist,
       a.date,
       a.add_time        AS addTime,
       a.modified_time   AS modifiedTime,
       a.total_duration  AS totalDuration
FROM main.albumArtists
         LEFT JOIN main.albums a on a.id = albumArtists.album
WHERE albumArtists.id = ?
ORDER BY 2
