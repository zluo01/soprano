SELECT a.id             AS album_id,
       a.name           AS album_name,
       (SELECT ar.name FROM albumArtists ar WHERE ar.album = a.id LIMIT 1) AS album_artist,
       a.date           as album_date,
       a.add_time       as album_add_time,
       a.modified_time  as album_modified_time,
       a.total_duration as album_total_duration,
       s.name           AS song_name,
       s.artists        AS song_artists,
       s.path           AS song_path,
       s.disc           AS song_disc,
       s.track_num      AS song_track_num,
       s.duration       AS song_duration
FROM albums a
         LEFT JOIN
     songs s ON a.id = s.album
WHERE a.id = ?
ORDER BY disc, track_num
