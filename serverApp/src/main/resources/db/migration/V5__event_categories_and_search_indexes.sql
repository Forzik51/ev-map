ALTER TABLE location
    DROP CONSTRAINT IF EXISTS location_has_point_or_region_ck;

ALTER TABLE location
    ADD CONSTRAINT location_has_point_or_region_ck
        CHECK (point IS NOT NULL OR region IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_location_point_gist ON location USING GIST (point);
CREATE INDEX IF NOT EXISTS idx_location_region_gist ON location USING GIST (region);

CREATE INDEX IF NOT EXISTS idx_category_event_event_id ON category_event (event_id);
CREATE INDEX IF NOT EXISTS idx_category_event_category_id ON category_event (category_id);

CREATE INDEX IF NOT EXISTS idx_event_name_lower ON event (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_event_description_lower ON event (LOWER(description));
CREATE INDEX IF NOT EXISTS idx_location_name_lower ON location (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_app_user_username_lower ON app_user (LOWER(username));
CREATE INDEX IF NOT EXISTS idx_app_user_name_lower ON app_user (LOWER(name));
