SELECT a.id,
       a.name,
       ar.name          AS artist,
       a.date,
       a.add_time       AS addTime,
       a.modified_time  AS modifiedTime,
       a.total_duration AS totalDuration
FROM genres
         LEFT JOIN main.albums a on a.id = genres.album
         LEFT JOIN albumArtists ar ON ar.album = a.id
WHERE genres.id = ?
ORDER BY 2
