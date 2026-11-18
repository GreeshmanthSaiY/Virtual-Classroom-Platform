-- =============================================================================
-- VIRTUAL CLASSROOM PLATFORM — MILESTONE 18.3 ROLLBACK SCRIPT
-- File:     docs/infrastructure/sql/M18_3_ROLLBACK.sql
-- Purpose:  Emergency rollback of Milestone 18.3 runtime RLS policies and
--           table/sequence ACL revocations while keeping RLS ENABLED on all
--           22 public tables.
-- Target:   Supabase STAGING ONLY (zrbpjcfssbtxxgrjtsca / postgres)
-- Executor: postgres
-- Note:     Before running this script, switch Render DATABASE_USERNAME back
--           to postgres.zrbpjcfssbtxxgrjtsca and restore its secret password.
-- =============================================================================

BEGIN;

-- 1. Drop Architecture A runtime RLS policies for vc_app_user
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
    FOREACH tbl IN ARRAY app_tables LOOP
        EXECUTE format('DROP POLICY IF EXISTS "vc_app_user_runtime_access" ON public.%I', tbl);
    END LOOP;
END $$;

-- 2. Revoke vc_app_user default privileges for postgres
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON TABLES FROM vc_app_user;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL ON SEQUENCES FROM vc_app_user;

-- 3. Restore prior Supabase default grants on the 22 tables & 18 sequences ONLY IF NEEDED
--    (RLS remains enabled with 0 policies, keeping anon/authenticated denied by PostgREST)
GRANT ALL ON TABLE
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
TO anon, authenticated;

GRANT ALL ON SEQUENCE
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
TO anon, authenticated;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT ALL ON TABLES TO anon, authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT ALL ON SEQUENCES TO anon, authenticated;

COMMIT;
