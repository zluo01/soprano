SELECT albums.id      as id,
       albums.name    as name,
       ar.name        as artist,
       date,
       add_time       as addTime,
       modified_time  as modifiedTime,
       total_duration as totalDuration
FROM main.albums
         LEFT JOIN
     albumArtists ar ON ar.album = albums.id
WHERE albums.name LIKE ?
ORDER BY 2, 1
