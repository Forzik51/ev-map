ALTER TABLE photo ADD COLUMN sort_order integer NOT NULL DEFAULT 0;
WITH positions AS (SELECT id, row_number() OVER(PARTITION BY event_id ORDER BY id)-1 AS position FROM photo) UPDATE photo p SET sort_order=positions.position FROM positions WHERE positions.id=p.id;
CREATE INDEX photo_order_idx ON photo(event_id,sort_order,id);
