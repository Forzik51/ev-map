ALTER TABLE event ALTER COLUMN name TYPE varchar(100), ALTER COLUMN description TYPE varchar(2000), ALTER COLUMN start_at DROP NOT NULL;
ALTER TABLE event ADD COLUMN date_mode varchar(16) NOT NULL DEFAULT 'DATETIME', ADD COLUMN starts_on date, ADD COLUMN ends_on date, ADD COLUMN time_zone varchar(80) NOT NULL DEFAULT 'UTC';
ALTER TABLE event ADD CONSTRAINT event_schedule_ck CHECK (
 (date_mode = 'UNSPECIFIED' AND start_at IS NULL AND end_at IS NULL AND starts_on IS NULL AND ends_on IS NULL) OR
 (date_mode = 'DATETIME' AND start_at IS NOT NULL AND starts_on IS NULL AND ends_on IS NULL AND (end_at IS NULL OR end_at >= start_at)) OR
 (date_mode = 'DATE' AND starts_on IS NOT NULL AND start_at IS NOT NULL AND (ends_on IS NULL OR ends_on >= starts_on) AND end_at IS NOT NULL)
);
CREATE INDEX event_created_paging_idx ON event(created_at DESC, id DESC);
