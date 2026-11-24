ALTER TABLE report ADD COLUMN status varchar(16) NOT NULL DEFAULT 'OPEN'
    CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'));

ALTER TABLE report_category ADD COLUMN is_selectable boolean NOT NULL DEFAULT true;

-- The previous moderation screen wrote these statuses into the category table.
UPDATE report_category
SET is_selectable = false
WHERE upper(btrim(report_type)) IN ('OPEN', 'RESOLVED');

UPDATE report r
SET status = upper(btrim(c.report_type))
FROM report_category c
WHERE c.id = r.category_id AND NOT c.is_selectable;

DO $$
DECLARE
    unrecovered_category_id bigint;
BEGIN
    IF to_regclass('report_legacy') IS NOT NULL THEN
        UPDATE report r
        SET category_id = original.id
        FROM report_legacy old
        JOIN report_category original ON original.id = old.kategoria_skarga_id AND original.is_selectable
        WHERE r.id = old.id
          AND r.event_id = old.wydarzenie_id
          AND r.user_id = old.uzytkownik_id
          AND EXISTS (SELECT 1 FROM report_category c WHERE c.id = r.category_id AND NOT c.is_selectable);
    END IF;

    IF EXISTS (
        SELECT 1 FROM report r JOIN report_category c ON c.id = r.category_id WHERE NOT c.is_selectable
    ) THEN
        INSERT INTO report_category(report_type, is_selectable)
        VALUES ('Category not recovered', false)
        RETURNING id INTO unrecovered_category_id;

        UPDATE report r
        SET category_id = unrecovered_category_id
        FROM report_category c
        WHERE c.id = r.category_id AND NOT c.is_selectable;
    END IF;
END
$$;

CREATE INDEX idx_report_status_id ON report (status, id DESC);

ALTER TABLE decision
    ALTER COLUMN description TYPE varchar(500),
    ALTER COLUMN blocked_until DROP NOT NULL,
    ALTER COLUMN moderator_id DROP NOT NULL,
    ADD COLUMN outcome varchar(16) CHECK (outcome IN ('RESOLVED', 'DISMISSED')),
    ADD COLUMN decided_at timestamptz,
    ADD CONSTRAINT decision_outcome_date_ck CHECK ((outcome IS NULL) = (decided_at IS NULL));

CREATE UNIQUE INDEX uq_report_moderation_decision ON decision (report_id) WHERE outcome IS NOT NULL;

ALTER TABLE decision DROP CONSTRAINT decision_report_fk;
ALTER TABLE decision ADD CONSTRAINT decision_report_fk
    FOREIGN KEY (report_id) REFERENCES report (id) ON DELETE CASCADE;

ALTER TABLE decision DROP CONSTRAINT decision_moderator_fk;
ALTER TABLE decision ADD CONSTRAINT decision_moderator_fk
    FOREIGN KEY (moderator_id) REFERENCES app_user (id) ON DELETE SET NULL;
