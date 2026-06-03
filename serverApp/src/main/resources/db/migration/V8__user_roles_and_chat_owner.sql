ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS role varchar(30) NOT NULL DEFAULT 'USER';

ALTER TABLE chat
    ADD COLUMN IF NOT EXISTS created_by_user_id bigint NULL;

UPDATE chat c
SET created_by_user_id = first_member.user_id
FROM (
    SELECT chat_id, MIN(user_id) AS user_id
    FROM user_chat
    GROUP BY chat_id
) first_member
WHERE c.id = first_member.chat_id
  AND c.created_by_user_id IS NULL;

ALTER TABLE chat
    ADD CONSTRAINT chat_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES app_user (id);

CREATE INDEX IF NOT EXISTS idx_chat_created_by_user_id ON chat (created_by_user_id);
