-- =============================================================================
-- VIRTUAL CLASSROOM PLATFORM — MILESTONE 18.3 REMEDIATION SCRIPT 1
-- File:     docs/infrastructure/sql/M18_3_1_provision_vc_app_user.sql
-- Purpose:  Idempotently provision unprivileged runtime role (vc_app_user) with
--           code-backed DML grants, 15 writable sequence grants, and explicit
--           Architecture A runtime RLS policies.
-- Target:   Supabase STAGING ONLY (zrbpjcfssbtxxgrjtsca / postgres)
-- Executor: postgres (tenant admin / migration identity)
-- Security: Password is NEVER hardcoded; supply via session parameter
--           `SET LOCAL vc.app_user_password = '...'` inside the transaction.
-- =============================================================================

BEGIN;

-- 1. Create or reconcile unprivileged login role (idempotent)
DO $$
DECLARE
    v_pwd text;
BEGIN
    v_pwd := current_setting('vc.app_user_password', true);
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'vc_app_user') THEN
        IF v_pwd IS NULL OR length(trim(v_pwd)) < 24 THEN
            RAISE EXCEPTION 'vc.app_user_password must be supplied via secure parameter (>= 24 chars) when creating vc_app_user';
        END IF;
        EXECUTE format(
            'CREATE ROLE vc_app_user WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS NOINHERIT CONNECTION LIMIT 20 PASSWORD %L',
            v_pwd
        );
    ELSE
        -- Verify existing role is NOT a superuser (PostgreSQL 16/17 requires SUPERUSER
        -- to pass SUPERUSER/NOSUPERUSER in ALTER ROLE even when already false)
        IF EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'vc_app_user' AND rolsuper) THEN
            RAISE EXCEPTION 'Security violation: existing vc_app_user must not have SUPERUSER attribute';
        END IF;
        -- Re-assert strict least-privilege role attributes on re-execution
        ALTER ROLE vc_app_user WITH
            LOGIN
            NOCREATEDB
            NOCREATEROLE
            NOBYPASSRLS
            NOINHERIT
            CONNECTION LIMIT 20;
        IF v_pwd IS NOT NULL AND length(trim(v_pwd)) >= 24 THEN
            EXECUTE format('ALTER ROLE vc_app_user WITH PASSWORD %L', v_pwd);
        END IF;
    END IF;
END $$;

-- 2. Schema privilege (USAGE only — strictly NO CREATE)
REVOKE ALL ON SCHEMA public FROM vc_app_user;
GRANT USAGE ON SCHEMA public TO vc_app_user;

-- 3. Clear any residual object grants on the 22 postgres-owned tables & 18 sequences
--    so repeated runs always converge to the exact approved least-privilege matrix
REVOKE ALL ON TABLE
    public.answers,
    public.assignment_submissions,
    public.assignments,
    public.audit_logs,
    public.courses,
    public.degrees,
    public.examination_results,
    public.examinations,
    public.faculty_profiles,
    public.faculty_subject_assignments,
    public.flyway_schema_history,
    public.live_sessions,
    public.notifications,
    public.password_reset_tokens,
    public.questions,
    public.refresh_tokens,
    public.student_profiles,
    public.student_subject_enrollments,
    public.study_materials,
    public.subjects,
    public.users,
    public.video_lectures
FROM vc_app_user;

REVOKE ALL ON SEQUENCE
    public.users_id_seq,
    public.questions_id_seq,
    public.degrees_id_seq,
    public.courses_id_seq,
    public.subjects_id_seq,
    public.study_materials_id_seq,
    public.video_lectures_id_seq,
    public.answers_id_seq,
    public.notifications_id_seq,
    public.audit_logs_id_seq,
    public.refresh_tokens_id_seq,
    public.student_subject_enrollments_id_seq,
    public.live_sessions_id_seq,
    public.assignments_id_seq,
    public.assignment_submissions_id_seq,
    public.examinations_id_seq,
    public.examination_results_id_seq,
    public.password_reset_tokens_id_seq
FROM vc_app_user;

-- 4. Code-backed DML grants on the 21 application tables
--    NOTE: public.flyway_schema_history intentionally receives ZERO grants.

-- 4a. Read-only academic reference catalog (2 tables)
GRANT SELECT ON
    public.degrees,
    public.subjects
TO vc_app_user;

-- 4b. Courses (SELECT + UPDATE for Hibernate Course.archived toggle; NO INSERT/DELETE)
GRANT SELECT, UPDATE ON
    public.courses
TO vc_app_user;

-- 4c. Append-only immutable audit trail (strictly NO UPDATE/DELETE)
GRANT SELECT, INSERT ON
    public.audit_logs
TO vc_app_user;

-- 4d. Faculty subject allocations (SELECT + INSERT)
GRANT SELECT, INSERT ON
    public.faculty_subject_assignments
TO vc_app_user;

-- 4e. Operational tables without hard-delete (11 tables)
GRANT SELECT, INSERT, UPDATE ON
    public.users,
    public.student_profiles,
    public.faculty_profiles,
    public.student_subject_enrollments,
    public.assignments,
    public.assignment_submissions,
    public.examinations,
    public.examination_results,
    public.live_sessions,
    public.notifications,
    public.password_reset_tokens
TO vc_app_user;

-- 4f. Operational tables with verified repository delete() calls (5 tables)
GRANT SELECT, INSERT, UPDATE, DELETE ON
    public.study_materials,
    public.video_lectures,
    public.questions,
    public.answers,
    public.refresh_tokens
TO vc_app_user;

-- 5. Sequence grants — strictly the 15 runtime-writable sequences
--    (degrees_id_seq, courses_id_seq, subjects_id_seq are intentionally withheld)
GRANT USAGE, SELECT ON SEQUENCE
    public.users_id_seq,
    public.refresh_tokens_id_seq,
    public.password_reset_tokens_id_seq,
    public.student_subject_enrollments_id_seq,
    public.study_materials_id_seq,
    public.video_lectures_id_seq,
    public.live_sessions_id_seq,
    public.questions_id_seq,
    public.answers_id_seq,
    public.assignments_id_seq,
    public.assignment_submissions_id_seq,
    public.examinations_id_seq,
    public.examination_results_id_seq,
    public.notifications_id_seq,
    public.audit_logs_id_seq
TO vc_app_user;

-- 6. Explicit runtime-role RLS policies (Architecture A: Trusted Backend + NOBYPASSRLS)
--    HONEST SECURITY NOTE: These policies DO NOT provide per-user row isolation.
--    Student/Faculty/Admin authorization remains enforced by Spring Security and
--    service-layer domain checks. Keeping NOBYPASSRLS ensures RLS remains active
--    for all roles and blocks access to any table without an explicit policy
--    (including public.flyway_schema_history).
DO $$
DECLARE
    tbl text;
    app_tables text[] := ARRAY[
        'answers', 'assignment_submissions', 'assignments', 'audit_logs',
        'courses', 'degrees', 'examination_results', 'examinations',
        'faculty_profiles', 'faculty_subject_assignments',
        'live_sessions', 'notifications', 'password_reset_tokens', 'questions',
        'refresh_tokens', 'student_profiles', 'student_subject_enrollments',
        'study_materials', 'subjects', 'users', 'video_lectures'
    ];
BEGIN
    -- Ensure no policy exists on flyway_schema_history for vc_app_user
    DROP POLICY IF EXISTS "vc_app_user_runtime_access" ON public.flyway_schema_history;

    FOREACH tbl IN ARRAY app_tables LOOP
        EXECUTE format('DROP POLICY IF EXISTS "vc_app_user_runtime_access" ON public.%I', tbl);
        EXECUTE format(
            'CREATE POLICY "vc_app_user_runtime_access" ON public.%I FOR ALL TO vc_app_user USING (true) WITH CHECK (true)',
            tbl
        );
    END LOOP;
END $$;

-- 7. Scoped conservative default privileges for future postgres-owned migrations
--    Grants SELECT only by default; future migrations must explicitly grant INSERT/UPDATE/DELETE
--    and RLS policies per table.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON TABLES FROM vc_app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON TABLES TO vc_app_user;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM vc_app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO vc_app_user;

COMMIT;
