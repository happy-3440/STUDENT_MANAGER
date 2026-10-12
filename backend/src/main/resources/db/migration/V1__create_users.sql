-- V1: users
-- UUID primary key, created_at/updated_at on every mutable table, as specified
-- in the Phase 1 architecture.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'STUDENT',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_format CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
);

-- Case-insensitive uniqueness: "a@b.com" and "A@B.com" are the same account.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
