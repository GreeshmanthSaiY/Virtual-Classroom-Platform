-- =============================================================================
-- VIRTUAL CLASSROOM PLATFORM — MILESTONE 18.3 REMEDIATION SCRIPT 2
-- File:     docs/infrastructure/sql/M18_3_2_revoke_app_object_acls.sql
-- Purpose:  Object-by-object REVOKE of broad anon/authenticated/PUBLIC grants
--           strictly on the 22 postgres-owned tables and 18 postgres-owned
--           sequences in public, plus scoped default privileges for postgres.
--           Preserves ALL Supabase-managed schemas (storage, auth, realtime,
--           extensions, pgbouncer, etc.), public.rls_auto_enable(), and RLS.
-- Target:   Supabase STAGING ONLY (zrbpjcfssbtxxgrjtsca / postgres)
-- Executor: postgres
-- =============================================================================

BEGIN;

-- 1. Explicitly enumerated REVOKE on the 22 postgres-owned tables in public
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
FROM anon, authenticated, PUBLIC;

-- 2. Explicitly enumerated REVOKE on the 18 postgres-owned sequences in public
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
FROM anon, authenticated, PUBLIC;

-- 3. Prevent future postgres-owned migrations from granting table/sequence ACLs
--    to anon/authenticated. Scoped strictly to FOR ROLE postgres & TABLES/SEQUENCES.
--    Does NOT alter ROUTINES or supabase_admin defaults.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON TABLES FROM anon, authenticated;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM anon, authenticated;

COMMIT;
