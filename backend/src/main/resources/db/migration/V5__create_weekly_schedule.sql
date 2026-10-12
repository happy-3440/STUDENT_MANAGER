-- Needed for the EXCLUDE constraint below (range + equality in one index).
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE weekly_schedule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id      UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,

    day_of_week     SMALLINT NOT NULL, -- 0 = Monday ... 6 = Sunday
    start_minutes   SMALLINT NOT NULL, -- minutes since midnight, 0-1439
    end_minutes     SMALLINT NOT NULL,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_schedule_day CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT ck_schedule_start CHECK (start_minutes BETWEEN 0 AND 1439),
    CONSTRAINT ck_schedule_end CHECK (end_minutes > start_minutes AND end_minutes <= 1440),

    -- The actual guarantee a student needs: you cannot have two classes at
    -- an overlapping time on the same day, enforced by the database itself
    -- - not just by application code that a bug or a race condition could
    -- bypass. int4range's upper bound is exclusive by default, which is
    -- exactly right for [start, end) time slots.
    CONSTRAINT ex_schedule_no_overlap EXCLUDE USING gist (
        user_id WITH =,
        day_of_week WITH =,
        int4range(start_minutes, end_minutes) WITH &&
    )
);

CREATE INDEX ix_schedule_subject ON weekly_schedule (subject_id);
CREATE INDEX ix_schedule_user_day ON weekly_schedule (user_id, day_of_week);
