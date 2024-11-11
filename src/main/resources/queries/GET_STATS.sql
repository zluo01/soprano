SELECT
    (SELECT COUNT(DISTINCT id) FROM albums) AS albums,
    (SELECT COUNT(DISTINCT path) FROM songs) AS songs,
    (SELECT COUNT(DISTINCT id) FROM artists) AS artists;
