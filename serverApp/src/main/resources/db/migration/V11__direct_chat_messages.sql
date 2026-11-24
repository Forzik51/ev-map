CREATE TABLE direct_chat (
    chat_id bigint PRIMARY KEY REFERENCES chat(id) ON DELETE CASCADE,
    user_low bigint NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    user_high bigint NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    UNIQUE (user_low, user_high),
    CHECK (user_low < user_high)
);

ALTER TABLE message ADD COLUMN client_message_id uuid;
CREATE UNIQUE INDEX uq_message_client_id ON message (chat_id, user_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE TABLE chat_attachment (
    message_id bigint PRIMARY KEY REFERENCES message(id) ON DELETE CASCADE,
    kind varchar(8) NOT NULL CHECK (kind IN ('IMAGE', 'FILE', 'VOICE')),
    file_name varchar(160) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes integer NOT NULL CHECK (size_bytes > 0),
    duration_ms bigint CHECK (duration_ms > 0),
    content bytea NOT NULL CHECK (octet_length(content) = size_bytes)
);

ALTER TABLE chat_read_state DROP CONSTRAINT chat_read_state_user_chat_fk;
ALTER TABLE chat_read_state ADD CONSTRAINT chat_read_state_user_chat_fk
    FOREIGN KEY (chat_id, user_id) REFERENCES user_chat (chat_id, user_id) ON DELETE CASCADE;
ALTER TABLE chat_read_state DROP CONSTRAINT chat_read_state_message_fk;
ALTER TABLE chat_read_state ADD CONSTRAINT chat_read_state_message_fk
    FOREIGN KEY (last_read_message_id) REFERENCES message (id) ON DELETE SET NULL;

ALTER TABLE chat DROP CONSTRAINT chat_created_by_user_fk;
ALTER TABLE chat ADD CONSTRAINT chat_created_by_user_fk
    FOREIGN KEY (created_by_user_id) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE refresh_token DROP CONSTRAINT refresh_token_user_fk;
ALTER TABLE refresh_token ADD CONSTRAINT refresh_token_user_fk
    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;
