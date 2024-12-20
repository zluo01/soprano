SELECT
    id,
    name,
    COUNT(album) AS albumCount
FROM
    genres
GROUP BY
    id, name
ORDER BY
    name;
