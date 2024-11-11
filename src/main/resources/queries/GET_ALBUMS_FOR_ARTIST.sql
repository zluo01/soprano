SELECT a.id,
       a.name,
       ar.name          AS artist,
       a.date,
       a.add_time       AS addTime,
       a.modified_time  AS modifiedTime,
       a.total_duration AS totalDuration
FROM main.artists
         LEFT JOIN main.albums a on a.id = artists.album
         LEFT JOIN albumArtists ar ON ar.album = a.id
WHERE artists.id = ?
ORDER BY 2
