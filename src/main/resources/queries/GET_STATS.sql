SELECT
    (SELECT COUNT(*) FROM albums) AS albums,
    (SELECT COUNT(*) FROM songs) AS songs,
    (SELECT COUNT(DISTINCT id) FROM artists) AS artists;
