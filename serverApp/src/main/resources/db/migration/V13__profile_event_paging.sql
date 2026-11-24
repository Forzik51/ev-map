CREATE INDEX idx_event_author_start_id ON event (user_id, start_at DESC, id DESC);
