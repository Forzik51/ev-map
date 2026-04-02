ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS path varchar(255);

CREATE INDEX IF NOT EXISTS idx_photo_event_id_v4 ON photo(event_id);
