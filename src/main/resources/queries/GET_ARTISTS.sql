SELECT
    id,
    name,
    COUNT(album) AS albumCount
FROM
    artists
GROUP BY
    id, name
ORDER BY
    name;
