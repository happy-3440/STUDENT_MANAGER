CREATE TABLE attendance_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id      UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    schedule_id     UUID NOT NULL REFERENCES weekly_schedule(id) ON DELETE CASCADE,

    class_date      DATE NOT NULL,
    status          VARCHAR(10) NOT NULL,
    reason          VARCHAR(120), -- only meaningful when status = CANCELLED

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'CANCELLED')),
    -- One mark per class instance. Re-marking the same class on the same
    -- date is an UPDATE (change your mind from Present to Absent), never a
    -- second row - this is what makes attendance percentages trustworthy.
    CONSTRAINT uq_attendance_instance UNIQUE (schedule_id, class_date)
);

CREATE INDEX ix_attendance_subject_date ON attendance_records (subject_id, class_date);
CREATE INDEX ix_attendance_user ON attendance_records (user_id);
