SELECT
    id,
    name,
    COUNT(album) AS albumCount
FROM
    albumArtists
GROUP BY
    id, name
ORDER BY
    name;
