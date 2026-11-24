DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM app_user GROUP BY LOWER(username) HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Resolve duplicate case-insensitive usernames before applying V10';
    END IF;
END;
$$;

CREATE UNIQUE INDEX uq_app_user_username_lower ON app_user (LOWER(username));
DROP INDEX IF EXISTS idx_app_user_username_lower;
