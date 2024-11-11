SELECT s.name,
       artists,
       a.id            as albumId,
       a.name          as album,
       path,
       s.date,
       genre,
       composer,
       performer,
       s.disc,
       track_num       as trackNum,
       duration,
       s.modified_time as modifiedTime,
       a.add_time      as addTime
FROM albums a
         LEFT JOIN
     songs s ON a.id = s.album
WHERE s.name LIKE ?
   OR artists LIKE ?
ORDER BY 2, 1
