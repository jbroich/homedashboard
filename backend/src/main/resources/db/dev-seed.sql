-- Populate a new local development database with chart-friendly sample data.
-- The predicate preserves data created by a developer or received through MQTT.
WITH sample_times AS (
    SELECT generate_series(
            date_trunc('hour', CURRENT_TIMESTAMP) - INTERVAL '30 days',
            date_trunc('hour', CURRENT_TIMESTAMP),
            INTERVAL '1 hour') AS ts
),
rooms(room, temperature_offset, humidity_offset) AS (
    VALUES
        ('LIVINGROOM', 1.0, 0.0),
        ('BEDROOM', -0.5, 3.0),
        ('OFFICE', 0.5, -2.0),
        ('KITCHEN', 1.5, 5.0),
        ('TOILET', 0.0, 7.0)
)
INSERT INTO measurement (room, temperature_c, humidity_percent, battery_percent, ts)
SELECT
    rooms.room,
    ROUND((20.0 + rooms.temperature_offset
            + SIN(EXTRACT(EPOCH FROM sample_times.ts) / 86400.0) * 1.5)::numeric, 2)::DOUBLE PRECISION,
    ROUND((45.0 + rooms.humidity_offset
            + COS(EXTRACT(EPOCH FROM sample_times.ts) / 86400.0) * 4.0)::numeric, 2)::DOUBLE PRECISION,
    80 + MOD(EXTRACT(HOUR FROM sample_times.ts)::INTEGER, 20),
    sample_times.ts
FROM sample_times
CROSS JOIN rooms
WHERE NOT EXISTS (SELECT 1 FROM measurement);
