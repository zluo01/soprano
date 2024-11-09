INSERT INTO albums(id, name, date, add_time, modified_time, total_duration)
VALUES (?, ?, ?, ?, ?, ?)
ON CONFLICT (id) DO NOTHING
