ALTER TABLE location ALTER COLUMN name TYPE varchar(200);
ALTER TABLE location ADD COLUMN kind varchar(24) NOT NULL DEFAULT 'PLACE', ADD COLUMN address varchar(500), ADD COLUMN shape geometry(Geometry,4326), ADD COLUMN created_by bigint REFERENCES app_user(id) ON DELETE SET NULL;
UPDATE location SET shape = COALESCE(region,point);
CREATE FUNCTION sync_location_shape() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.shape IS NULL THEN NEW.shape := COALESCE(NEW.region,NEW.point);
 ELSIF TG_OP='UPDATE' AND NEW.shape IS NOT DISTINCT FROM OLD.shape AND (NEW.point IS DISTINCT FROM OLD.point OR NEW.region IS DISTINCT FROM OLD.region) THEN NEW.shape := COALESCE(NEW.region,NEW.point);
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER location_shape_sync BEFORE INSERT OR UPDATE ON location FOR EACH ROW EXECUTE FUNCTION sync_location_shape();
ALTER TABLE location ALTER COLUMN shape SET NOT NULL;
ALTER TABLE location ADD CONSTRAINT location_shape_ck CHECK (ST_IsValid(shape) AND NOT ST_IsEmpty(shape) AND GeometryType(shape) IN ('POINT','LINESTRING','MULTILINESTRING','POLYGON','MULTIPOLYGON'));
CREATE INDEX location_shape_idx ON location USING gist(shape);
CREATE INDEX location_shape_distance_idx ON location USING gist((shape::geography));
CREATE TABLE geocoding_cache(cache_key varchar(64) PRIMARY KEY, payload text NOT NULL, expires_at timestamptz NOT NULL);
CREATE TABLE location_candidate(id uuid PRIMARY KEY, user_id bigint NOT NULL REFERENCES app_user(id) ON DELETE CASCADE, payload text NOT NULL, expires_at timestamptz NOT NULL, location_id bigint REFERENCES location(id) ON DELETE SET NULL);
INSERT INTO location_category(name) SELECT 'Місце' WHERE NOT EXISTS(SELECT 1 FROM location_category WHERE name='Місце');
