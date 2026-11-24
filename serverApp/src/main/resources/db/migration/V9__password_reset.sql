-- Fail safely if legacy addresses differ only in case: resolve duplicates before migrating.
CREATE UNIQUE INDEX IF NOT EXISTS app_user_email_lower_uk ON app_user (LOWER(email));
ALTER TABLE app_user ADD COLUMN auth_version bigint NOT NULL DEFAULT 0;

CREATE TABLE password_reset_request (
    id uuid PRIMARY KEY,
    user_id bigint REFERENCES app_user(id) ON DELETE CASCADE,
    identifier_hash varchar(64) NOT NULL,
    ip_hash varchar(64) NOT NULL,
    code_hash varchar(64) NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    failed_attempts integer NOT NULL DEFAULT 0,
    verified_at timestamptz,
    reset_token_hash varchar(64) UNIQUE,
    reset_expires_at timestamptz,
    used_at timestamptz,
    invalidated_at timestamptz
);
CREATE INDEX password_reset_identifier_created_idx ON password_reset_request(identifier_hash, created_at DESC);
CREATE INDEX password_reset_ip_created_idx ON password_reset_request(ip_hash, created_at DESC);
CREATE INDEX password_reset_user_idx ON password_reset_request(user_id);

-- The body contains a code, so encrypt it with a separately configured application key.
CREATE TABLE password_reset_mail_outbox (
    id uuid PRIMARY KEY,
    request_id uuid REFERENCES password_reset_request(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    recipient varchar(254) NOT NULL,
    subject varchar(200) NOT NULL,
    encrypted_body text NOT NULL,
    key_id varchar(64) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    sent_at timestamptz,
    failed_at timestamptz
);
CREATE INDEX password_reset_mail_pending_idx ON password_reset_mail_outbox(available_at)
    WHERE sent_at IS NULL AND failed_at IS NULL;
