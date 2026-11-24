-- Run explicitly against a local dev database after Flyway has initialized it.
SET search_path TO evmap, public;

WITH account AS (
    INSERT INTO app_user(name, email, phone, birthdate, password_hash, username, page_description)
    VALUES ('Demo', 'demo@evmap.invalid', '+48000000000', DATE '1990-01-01',
            '$2a$10$/Qyt6pddKvNXo2EDivPjBO.PIA4Xiy.vkACnKWLSxf5ILvUT5vc12',
            'evmap-demo', 'Local demonstration account')
    ON CONFLICT DO NOTHING
    RETURNING id
), venue AS (
    INSERT INTO location(name, category_id, point)
    SELECT 'Warsaw demo park', c.id, ST_SetSRID(ST_MakePoint(21.0122, 52.2297), 4326)
    FROM account CROSS JOIN (SELECT id FROM location_category WHERE name = 'Park' ORDER BY id LIMIT 1) c
    RETURNING id
), demo_event AS (
    INSERT INTO event(name, description, location_id, start_at, end_at, created_at, user_id)
    SELECT 'Demo concert', 'Sample event for local development', venue.id, now() + interval '7 days',
           now() + interval '7 days 2 hours', now(), account.id
    FROM account CROSS JOIN venue
    RETURNING id
)
INSERT INTO category_event(category_id, event_id)
SELECT c.id, demo_event.id FROM demo_event
CROSS JOIN (SELECT id FROM category WHERE name = 'Koncert' ORDER BY id LIMIT 1) c;
