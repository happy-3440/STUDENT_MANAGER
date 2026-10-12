-- A subject's 100 marks: quiz/tutorial (10, optional) + assignments (10,
-- see V7) + attendance (5, computed from attendance_records) + mid (15) +
-- end (60). Quiz/tutorial and mid/end are 1:1 attributes of the subject
-- itself, so they live inline here rather than in a join table.

CREATE TABLE subjects (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    semester_id             UUID NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,

    name                    VARCHAR(100) NOT NULL,
    teacher                 VARCHAR(100) NOT NULL,
    required_attendance_pct SMALLINT NOT NULL DEFAULT 75,
    credits                 NUMERIC(3,1),

    quiz_on                 BOOLEAN NOT NULL DEFAULT false,
    quiz_marks              NUMERIC(4,1),
    tutorial_on             BOOLEAN NOT NULL DEFAULT false,
    tutorial_marks          NUMERIC(4,1),
    mid_marks               NUMERIC(4,1),
    end_marks               NUMERIC(5,1),

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_subjects_req_att CHECK (required_attendance_pct BETWEEN 50 AND 100),
    CONSTRAINT ck_subjects_credits CHECK (credits IS NULL OR credits BETWEEN 0.5 AND 10),
    CONSTRAINT ck_subjects_quiz CHECK (quiz_marks IS NULL OR quiz_marks BETWEEN 0 AND 10),
    CONSTRAINT ck_subjects_tutorial CHECK (tutorial_marks IS NULL OR tutorial_marks BETWEEN 0 AND 10),
    CONSTRAINT ck_subjects_quiz_tutorial_total CHECK (
        COALESCE(quiz_marks, 0) + COALESCE(tutorial_marks, 0) <= 10
    ),
    CONSTRAINT ck_subjects_mid CHECK (mid_marks IS NULL OR mid_marks BETWEEN 0 AND 15),
    CONSTRAINT ck_subjects_end CHECK (end_marks IS NULL OR end_marks BETWEEN 0 AND 60)
);

CREATE INDEX ix_subjects_user ON subjects (user_id);
CREATE INDEX ix_subjects_semester ON subjects (semester_id);
