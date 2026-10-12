CREATE TABLE semesters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    number      SMALLINT NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_semesters_number CHECK (number BETWEEN 1 AND 12),
    CONSTRAINT ck_semesters_dates CHECK (end_date > start_date),
    -- One row per semester number per user. Re-saving the same semester is
    -- an update, not a new row.
    CONSTRAINT uq_semesters_user_number UNIQUE (user_id, number)
);

CREATE INDEX ix_semesters_user ON semesters (user_id);
