CREATE TABLE IF NOT EXISTS refresh_token (
    token_hash varchar(64) NOT NULL,
    user_id bigint NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    revoked_at timestamptz NULL,
    CONSTRAINT refresh_token_pk PRIMARY KEY (token_hash),
    CONSTRAINT refresh_token_user_fk FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_token (expires_at);

CREATE TABLE IF NOT EXISTS chat_read_state (
    chat_id bigint NOT NULL,
    user_id bigint NOT NULL,
    last_read_message_id bigint NULL,
    read_at timestamptz NOT NULL,
    CONSTRAINT chat_read_state_pk PRIMARY KEY (chat_id, user_id),
    CONSTRAINT chat_read_state_user_chat_fk FOREIGN KEY (chat_id, user_id) REFERENCES user_chat (chat_id, user_id),
    CONSTRAINT chat_read_state_message_fk FOREIGN KEY (last_read_message_id) REFERENCES message (id)
);

CREATE INDEX IF NOT EXISTS idx_message_chat_sent_id ON message (chat_id, sent_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_event_start_at ON event (start_at);
CREATE INDEX IF NOT EXISTS idx_location_category_id_v6 ON location (category_id);
