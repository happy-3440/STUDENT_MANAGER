# Student Manager — backend foundation (Phase 3 + 4)

This is real, runnable source code — not a prototype. It implements the
piece you asked about directly: **accounts stored in a real PostgreSQL
database, with login that works identically from any device**, because
every device talks to the same server and the same database row.

I wrote and reviewed this code carefully, but I have no network access in
this environment, so I could not actually run `mvn` or `docker compose up`
myself to compile or execute it. Run the verification steps below yourself
before relying on it — that's not optional, it's the only way either of us
knows it actually works.

## What's in here

```
backend/            Java 21 + Spring Boot 3 + PostgreSQL + JWT auth
  src/main/...       application code
  src/test/...        a REAL integration test (see below)
infrastructure/      docker-compose.yml + .env.example
```

## What this phase does NOT include yet

Subjects, attendance, tasks, notes — everything else the prototype's UI
shows — still only exists in the browser prototype's `localStorage`. This
phase is specifically: accounts, login, and the database they live in.
Once you confirm this works, the next phase connects the rest of the data
(subjects, attendance, etc.) to this same `users` table, each row owned by
a `user_id`, the same way `refresh_tokens` already is.

## Run it yourself

1. Install Docker and Docker Compose.
2. `cd infrastructure`
3. `cp .env.example .env`, then fill in:
   - `DATABASE_PASSWORD` — any strong password
   - `JWT_SECRET` — run `openssl rand -base64 48` and paste the output
4. `docker compose up --build`
5. Wait for `backend` to report healthy. Flyway runs the migrations
   automatically on startup — you do not run SQL by hand.

## Verify the actual claim — not just that it boots

This is the part that matters. "It starts" is not the same as "it works."

```bash
# 1. Register on "device A"
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Soumya","email":"you@example.com","password":"a-strong-password"}' | jq

# Copy the "id" under data.user from the response above.

# 2. Log in on "device B" — a separate curl call, no shared state
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"a-strong-password"}' | jq

# Confirm data.user.id is IDENTICAL in both responses. That equality is
# the actual proof: two independent logins resolve to the same account.

# 3. Confirm the access token from device B actually authenticates as
# that same user against a protected endpoint:
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken from step 2>" | jq
```

Also try registering with the same email twice — the second call should
return `409 Conflict`, not a second account.

## Run the automated test

`CrossDeviceLoginTest.java` runs this exact scenario automatically,
against a real throwaway PostgreSQL container (via Testcontainers) — not
mocks. It needs Docker running, because Testcontainers starts a real
database for the test.

```bash
cd backend
mvn test -Dtest=CrossDeviceLoginTest
```

If this test fails, something is genuinely broken — tell me the failure
output and I'll fix the actual cause, not just the symptom.

## About "pushing a fix once it's launched"

Once this is deployed somewhere real (a VPS, Railway, Render, etc.):
- A backend bug (wrong SGPA formula, a security issue) — I fix the Java
  code, you redeploy this container. Every device gets the fix
  immediately, with no app update needed.
- A mobile UI bug — that's compiled into the app itself, so it needs an
  app update through the Play Store / App Store, which is slower. Feature
  flags or a tool like Shorebird (for Flutter) can patch some things
  without waiting on app-store review.

This backend doesn't do either of those yet — it's the foundation they'd
sit on top of.

## Phase 2: Academics + Attendance (just added)

Real tables, endpoints, and business logic for semesters, subjects,
weekly timetables, attendance marking, assignments, and SGPA — all owned
per-user, same pattern as auth.

**New tables:** `semesters`, `subjects`, `weekly_schedule`,
`attendance_records`, `assignments`. Flyway migrations V3–V7.

**Two real guarantees worth knowing about:**
- **No double-booked classes.** The `weekly_schedule` table has a
  database-level `EXCLUDE` constraint — two overlapping time slots on the
  same day, for the same user, are physically impossible to insert, not
  just blocked by application code that a bug could route around.
- **SGPA is computed once, server-side.** The 100-mark breakdown (quiz/
  tutorial 10, assignments 10 split evenly, attendance 5, mid 15, end 60)
  and the 10-point grade scale live in `GradeCalculator.java` — the
  single source of truth. No client re-derives this math.

**Endpoints added**, all under `/api/v1`, all requiring login:
```
POST/GET   /semesters
GET        /semesters/{id}/dashboard          (SGPA + subject list)
POST/GET   /semesters/{id}/subjects
PUT        /subjects/{id}                      (name, teacher, required %)
PUT        /subjects/{id}/marks                (credits, quiz/tutorial, mid, end)
DELETE     /subjects/{id}
POST/GET   /subjects/{id}/schedule
GET        /schedule/day/{0-6}                 (Monday=0 ... Sunday=6)
DELETE     /schedule/{id}
POST       /attendance                         (mark present/absent/cancelled)
DELETE     /attendance/schedule/{id}?date=...   (un-mark)
GET        /attendance/subject/{id}             (percentage + recovery count)
POST/GET   /subjects/{id}/assignments
PATCH/DELETE /assignments/{id}
```

**Run the new tests** the same way as before:
```bash
cd backend
mvn test -Dtest=AcademicAndAttendanceTest
```
This test actually tries to double-book a class and asserts it's
rejected with `409`, and separately creates subjects with real marks and
checks the SGPA comes out to the exact expected value (credit-weighted,
rounded correctly) — not just that the endpoint returns `200`.

**Deliberately deferred to a later pass** (so you know they're not
silently missing, just not built yet): weekly holidays, per-date class
cancellation beyond the basic `reason` field already in
`attendance_records`, marking several classes at once in one request,
and CGPA across multiple past semesters (SGPA per semester is in; the
cross-semester rollup isn't yet).

## What's genuinely still missing before this is production-ready

Being direct about the gaps, so nothing here is overstated:
- No rate limiting on login/register yet (brute-force protection).
- No email verification or password reset flow.
- No refresh-token cleanup job for expired/revoked rows (they'll
  accumulate — fine for now, not fine forever).
- No CI, no HTTPS termination (Nginx/Let's Encrypt) configured yet.
- `JWT_SECRET` must be a real secret you generate and keep safe — the
  app refuses to start without one, on purpose.

Tell me which of these to build next, or if the cross-device login above
doesn't check out when you run it, tell me exactly what happened.
