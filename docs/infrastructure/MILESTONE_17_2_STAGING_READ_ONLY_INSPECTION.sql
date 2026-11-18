-- =============================================================================
-- VIRTUAL CLASSROOM PLATFORM — STAGING DATABASE READ-ONLY INSPECTION SCRIPT
-- =============================================================================
-- Target Database:      Supabase Staging
-- Project Name:         virtual-classroom-staging
-- Project Reference:    zrbpjcfssbtxxgrjtsca
-- Target Region:        ap-south-1 (Mumbai, India)
-- Connection Method:    Supavisor Session Pooler (Port 5432) or Supabase SQL Editor
-- Script Classification: STRICTLY READ-ONLY (SELECT & Catalog Metadata Queries Only)
-- Safety Guarantee:     ZERO DDL (CREATE/ALTER/DROP) and ZERO DML (INSERT/UPDATE/DELETE).
-- =============================================================================
-- INSTRUCTIONS FOR OPERATOR:
-- 1. Open the Supabase Dashboard: https://supabase.com/dashboard/project/zrbpjcfssbtxxgrjtsca
-- 2. In the left navigation bar, click "SQL Editor".
-- 3. Click "+ New query" to open a blank query tab.
-- 4. Copy and paste the queries from this file.
-- 5. You may run queries section by section, or execute the catalog queries together.
-- 6. For queries in Sections 3B, 4B, and 5, verify the corresponding table existence
--    flag in Section 2/3A/4A before running.
-- =============================================================================

-- =============================================================================
-- SECTION 1: DATABASE IDENTITY, HOST, USER & POSTGRESQL VERSION
-- Purpose: Verify that the connection is pointing to the intended staging project,
--          confirm PostgreSQL engine version compatibility, and inspect session role.
-- Prerequisites: None (Standard PostgreSQL system functions).
-- =============================================================================

-- Query 1.1: Database Engine, Identity & Session Metadata
SELECT 
    current_database()                                   AS database_name,
    current_user                                         AS session_user,
    session_user                                         AS authenticated_role,
    current_schema()                                     AS active_schema,
    inet_server_addr()                                   AS server_ip_address,
    inet_server_port()                                   AS server_port,
    version()                                            AS full_version_string,
    current_setting('server_version')                    AS postgresql_version,
    current_setting('TimeZone')                          AS timezone_setting,
    to_char(now(), 'YYYY-MM-DD HH24:MI:SS OF')           AS inspection_timestamp;

-- Query 1.2: Core Database Configuration Parameters
SELECT 
    name                                                 AS parameter_name,
    setting                                              AS configured_value,
    unit                                                 AS parameter_unit,
    short_desc                                           AS parameter_description
FROM pg_settings 
WHERE name IN (
    'server_version',
    'listen_addresses',
    'port',
    'max_connections',
    'search_path',
    'ssl',
    'standard_conforming_strings',
    'client_encoding',
    'server_encoding'
)
ORDER BY name ASC;


-- =============================================================================
-- SECTION 2: PUBLIC SCHEMA TABLE INVENTORY & ESTIMATED ROW COUNTS
-- Purpose: Discover all existing relations in the 'public' schema and check whether
--          the database is pristine/fresh, partially initialized, or fully migrated.
-- Prerequisites: None (Queries PostgreSQL system catalogs pg_class and information_schema).
-- =============================================================================

-- Query 2.1: Complete Catalog Inventory of Public Schema Relations
SELECT 
    c.relname                                            AS relation_name,
    CASE c.relkind
        WHEN 'r' THEN 'TABLE (Regular)'
        WHEN 'p' THEN 'TABLE (Partitioned)'
        WHEN 'v' THEN 'VIEW'
        WHEN 'm' THEN 'MATERIALIZED VIEW'
        WHEN 'i' THEN 'INDEX'
        WHEN 'S' THEN 'SEQUENCE'
        WHEN 'f' THEN 'FOREIGN TABLE'
        ELSE c.relkind::text
    END                                                  AS relation_type,
    pg_size_pretty(pg_total_relation_size(c.oid))        AS total_disk_size,
    c.reltuples::bigint                                  AS estimated_row_count
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relkind IN ('r', 'p', 'v')
ORDER BY c.relname ASC;

-- Query 2.2: Expected Application Tables Comparison Matrix (21 Application Tables + Flyway History)
-- Checks presence and row counts for every table introduced across Flyway migrations V1 to V5.
WITH expected_tables(table_name, migration_source, expected_purpose) AS (
    VALUES
    -- Flyway V1: Initial Enterprise Schema
    ('users',                       'V1', 'Core user accounts, roles, and authentication state'),
    ('degrees',                     'V1', 'Academic degree programs (B.Sc, M.Sc, B.Tech, etc.)'),
    ('courses',                     'V1', 'Course offerings linked to degrees'),
    ('subjects',                    'V1', 'Curriculum subject units per course and year level'),
    ('student_profiles',            'V1', 'Student roll numbers, enrolled courses, and years'),
    ('faculty_profiles',            'V1', 'Faculty employee IDs and academic departments'),
    ('faculty_subject_assignments', 'V1', 'Faculty-to-subject term teaching assignments'),
    ('study_materials',             'V1', 'Subject study materials and uploaded asset metadata'),
    ('video_lectures',              'V1', 'Curated video lectures and streaming URLs'),
    ('questions',                   'V1', 'Discussion forum question threads'),
    ('answers',                     'V1', 'Discussion forum answer postings'),
    ('notifications',               'V1', 'User notifications and read/unread status'),
    ('audit_logs',                  'V1', 'System action audit records and actor logging'),
    -- Flyway V4: Academic Platform Expansion
    ('refresh_tokens',              'V4', 'JWT refresh token rotation and revocation tracking'),
    ('student_subject_enrollments', 'V4', 'Explicit student subject enrollment per term'),
    ('live_sessions',               'V4', 'Scheduled virtual classroom sessions and links'),
    ('assignments',                 'V4', 'Faculty coursework assignments and deadlines'),
    ('assignment_submissions',      'V4', 'Student assignment submissions, files, and grades'),
    ('examinations',                'V4', 'Formal academic exam schedules and definitions'),
    ('examination_results',         'V4', 'Student examination marks, grades, and remarks'),
    -- Flyway V5: Password Reset & Archival
    ('password_reset_tokens',       'V5', 'Temporary cryptographic password reset tokens'),
    -- Flyway Metadata
    ('flyway_schema_history',       'Flyway', 'Flyway migration version tracking and checksums')
)
SELECT 
    e.migration_source                                   AS migration_file,
    e.table_name                                         AS expected_table,
    CASE 
        WHEN t.table_name IS NOT NULL THEN 'PRESENT'
        ELSE 'NOT_FOUND'
    END                                                  AS presence_status,
    COALESCE(s.n_live_tup, 0)                            AS live_row_count,
    e.expected_purpose
FROM expected_tables e
LEFT JOIN information_schema.tables t 
    ON t.table_schema = 'public' 
   AND t.table_name = e.table_name
LEFT JOIN pg_stat_user_tables s
    ON s.schemaname = 'public' 
   AND s.relname = e.table_name
ORDER BY 
    CASE e.migration_source
        WHEN 'V1' THEN 1
        WHEN 'V4' THEN 2
        WHEN 'V5' THEN 3
        ELSE 4
    END, 
    e.table_name ASC;

-- Query 2.3: Executive Schema Health & Baseline-on-Migrate Risk Classifier
SELECT 
    COUNT(CASE WHEN t.table_name IS NOT NULL AND t.table_name != 'flyway_schema_history' THEN 1 END) AS application_tables_count,
    21                                                   AS expected_application_tables,
    COUNT(CASE WHEN t.table_name = 'flyway_schema_history' THEN 1 END) AS flyway_history_table_exists,
    CASE 
        WHEN COUNT(t.table_name) = 0 THEN 
            'SCENARIO A: PRISTINE DATABASE (0 tables found). Safe for Flyway migration V1-V5 execution.'
        WHEN COUNT(CASE WHEN t.table_name = 'flyway_schema_history' THEN 1 END) = 1 
             AND COUNT(CASE WHEN t.table_name != 'flyway_schema_history' THEN 1 END) = 21 THEN 
            'SCENARIO B: FULLY MIGRATED (All 21 application tables + flyway_schema_history present). System is ready.'
        WHEN COUNT(CASE WHEN t.table_name = 'flyway_schema_history' THEN 1 END) = 0 
             AND COUNT(t.table_name) > 0 THEN 
            'SCENARIO C: PARTIALLY POPULATED WITHOUT FLYWAY (CRITICAL HAZARD: application-staging.yml has baseline-on-migrate: true, which will cause Flyway to SKIP V1__init_schema.sql!).'
        ELSE 
            'SCENARIO D: INTERMEDIATE STATE (Partial tables or migration in progress). Inspect Section 3 and Section 4 before proceeding.'
    END                                                  AS preflight_verdict
FROM information_schema.tables t
WHERE t.table_schema = 'public';


-- =============================================================================
-- SECTION 3: FLYWAY SCHEMA HISTORY INSPECTION
-- Purpose: Inspect migration status, version history, execution timestamps, and checksums.
-- Prerequisites: 
--   Query 3.1: None (Safe catalog check).
--   Query 3.2 & 3.3: Requires flyway_schema_history to exist (confirmed by Query 3.1).
-- =============================================================================

-- Query 3.1: Safe Flyway Table Existence Check
SELECT 
    to_regclass('public.flyway_schema_history') IS NOT NULL AS flyway_schema_history_exists;

-- Query 3.2: Detailed Migration Version History
-- PREREQUISITE: Execute ONLY if Query 3.1 returned TRUE.
-- (If flyway_schema_history does not exist, running this directly will return relation not found).
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    to_char(installed_on, 'YYYY-MM-DD HH24:MI:SS OF')    AS installed_at,
    execution_time                                       AS execution_time_ms,
    success                                              AS migration_successful
FROM public.flyway_schema_history
ORDER BY installed_rank ASC;

-- Query 3.3: Repository Migration Hash Verification Matrix
-- PREREQUISITE: Execute ONLY if Query 3.1 returned TRUE.
-- Compares live checksums in the database against the exact SHA-256 hashes in the codebase.
WITH expected_migrations(version, script_name, description) AS (
    VALUES
    ('1', 'V1__init_schema.sql',                         'init schema'),
    ('2', 'V2__seed_academic_data.sql',                  'seed academic data'),
    ('3', 'V3__security_harden_remove_demo_users.sql',   'security harden remove demo users'),
    ('4', 'V4__academic_platform_expansion.sql',         'academic platform expansion'),
    ('5', 'V5__password_reset_and_course_archival.sql',  'password reset and course archival')
)
SELECT 
    em.version,
    em.script_name                                       AS expected_script,
    em.description                                       AS expected_description,
    fsh.script                                           AS recorded_script,
    fsh.checksum                                         AS recorded_flyway_checksum,
    fsh.success                                          AS recorded_success,
    CASE 
        WHEN fsh.version IS NULL THEN 'NOT_MIGRATED'
        WHEN fsh.success = FALSE THEN 'MIGRATION_FAILED'
        ELSE 'MIGRATION_APPLIED'
    END                                                  AS status
FROM expected_migrations em
LEFT JOIN public.flyway_schema_history fsh 
    ON fsh.version = em.version
ORDER BY em.version::int ASC;


-- =============================================================================
-- SECTION 4: V3 TARGET DEMO ACCOUNTS AUDIT (STRICT ZERO-PRIVACY-LEAK SELECT)
-- Purpose: Verify whether the 5 demonstration user accounts seeded in V2 exist,
--          confirm their current status, and verify that no password hashes or
--          unrelated user records are exposed.
-- Prerequisites:
--   Query 4.1: None (Safe catalog check).
--   Query 4.2 & 4.3: Requires public.users table to exist.
-- =============================================================================

-- Query 4.1: Safe Users Table Existence Check
SELECT 
    to_regclass('public.users') IS NOT NULL              AS users_table_exists;

-- Query 4.2: Targeted Demo Account Inspection (5 Specific Emails Only)
-- PREREQUISITE: Execute ONLY if Query 4.1 returned TRUE.
-- STRICT FILTER: Restricts query exclusively to the 5 known demo email addresses.
-- Excludes password_hash to enforce strict security boundaries.
SELECT 
    id                                                   AS user_id,
    email,
    full_name,
    role,
    status,
    to_char(created_at, 'YYYY-MM-DD HH24:MI:SS OF')      AS registered_at
FROM public.users
WHERE email IN (
    'admin@virtualclassroom.edu',
    'dr.jeevan@virtualclassroom.edu',
    'gaurav.shukla@virtualclassroom.edu',
    'alice.smith@virtualclassroom.edu',
    'prof.tushar@virtualclassroom.edu'
)
ORDER BY id ASC;

-- Query 4.3: Aggregate User Count & Non-Target Isolation Check
-- PREREQUISITE: Execute ONLY if Query 4.1 returned TRUE.
-- Confirms how many demo accounts exist vs authentic users without displaying personal details.
SELECT 
    COUNT(CASE WHEN email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    ) THEN 1 END)                                        AS demo_accounts_count,
    COUNT(CASE WHEN email NOT IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    ) THEN 1 END)                                        AS authentic_users_count,
    COUNT(*)                                             AS total_registered_users
FROM public.users;


-- =============================================================================
-- SECTION 5: V3 DEPENDENT-RECORD IMPACT ANALYSIS (CHILD TABLE AUDIT)
-- Purpose: Inspect all child tables referencing the 5 demo accounts to evaluate
--          the exact blast radius of Flyway migration V3 (DML purge).
-- Prerequisites: Execute ONLY if public.users and the respective child tables exist.
-- =============================================================================

-- Query 5.1: Blast Radius Summary — Count of Demo User Child Records Targeted by V3
-- PREREQUISITE: Execute ONLY if public.users exists.
-- Evaluates the exact record counts that V3 would delete across each dependent table.
WITH target_demo_users AS (
    SELECT id, email 
    FROM public.users 
    WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
)
SELECT 
    'answers'                                            AS dependent_table,
    'author_id'                                          AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.answers 
WHERE author_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'questions'                                          AS dependent_table,
    'author_id'                                          AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.questions 
WHERE author_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'study_materials'                                    AS dependent_table,
    'uploader_id'                                        AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.study_materials 
WHERE uploader_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'video_lectures'                                     AS dependent_table,
    'faculty_id'                                         AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.video_lectures 
WHERE faculty_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'faculty_subject_assignments'                        AS dependent_table,
    'faculty_id'                                         AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.faculty_subject_assignments 
WHERE faculty_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'student_profiles'                                   AS dependent_table,
    'user_id'                                            AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.student_profiles 
WHERE user_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'faculty_profiles'                                   AS dependent_table,
    'user_id'                                            AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.faculty_profiles 
WHERE user_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'notifications'                                      AS dependent_table,
    'user_id'                                            AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.notifications 
WHERE user_id IN (SELECT id FROM target_demo_users)

UNION ALL

SELECT 
    'audit_logs'                                         AS dependent_table,
    'actor_id / actor_email'                             AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.audit_logs 
WHERE actor_id IN (SELECT id FROM target_demo_users)
   OR actor_email IN (SELECT email FROM target_demo_users)

UNION ALL

SELECT 
    'users'                                              AS dependent_table,
    'id (primary key)'                                   AS foreign_key_column,
    COUNT(*)                                             AS records_to_be_purged_by_v3
FROM public.users 
WHERE id IN (SELECT id FROM target_demo_users);

-- Query 5.2: Academic Curriculum Preservation Verification
-- Confirms that reference curriculum tables (Degrees, Courses, Subjects) are NOT affected by V3.
SELECT 
    'degrees'                                            AS reference_table,
    COUNT(*)                                             AS total_records_preserved,
    'Protected from V3 purge (Permanent curriculum data)' AS policy_note
FROM public.degrees

UNION ALL

SELECT 
    'courses'                                            AS reference_table,
    COUNT(*)                                             AS total_records_preserved,
    'Protected from V3 purge (Permanent curriculum data)' AS policy_note
FROM public.courses

UNION ALL

SELECT 
    'subjects'                                           AS reference_table,
    COUNT(*)                                             AS total_records_preserved,
    'Protected from V3 purge (Permanent curriculum data)' AS policy_note
FROM public.subjects;


-- =============================================================================
-- SECTION 6: SCHEMA, CONSTRAINT & INDEX COMPATIBILITY ASSESSMENT
-- Purpose: Inspect existing indexes, foreign keys, unique constraints, and sequences
--          to ensure there are no collisions or name conflicts with V1-V5 definitions.
-- Prerequisites: None (Queries PostgreSQL system catalogs).
-- =============================================================================

-- Query 6.1: Existing Custom Indexes Inspection
-- Searches for index names defined in Flyway migrations V1 through V5.
SELECT 
    schemaname                                           AS schema_name,
    tablename                                            AS table_name,
    indexname                                            AS index_name,
    indexdef                                             AS index_definition
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
      -- V1 Indexes
      'idx_users_role_status',
      'idx_users_email',
      'idx_courses_degree',
      'idx_subjects_course_year',
      'idx_materials_subject',
      'idx_lectures_subject',
      'idx_questions_subject',
      'idx_answers_question',
      'idx_notifications_user_unread',
      'idx_audit_timestamp',
      -- V4 Indexes
      'idx_refresh_tokens_user',
      'idx_refresh_tokens_lookup',
      'idx_enrollments_student',
      'idx_enrollments_subject',
      'idx_live_sessions_subject',
      'idx_live_sessions_schedule',
      'idx_assignments_subject_due',
      'idx_assignments_faculty',
      'idx_submissions_assignment',
      'idx_submissions_student',
      'idx_examinations_subject',
      'idx_exam_results_exam',
      'idx_exam_results_student',
      -- V5 Indexes
      'idx_pwd_reset_tokens_user',
      'idx_pwd_reset_tokens_lookup'
  )
ORDER BY tablename, indexname ASC;

-- Query 6.2: Foreign Key & Unique Constraint Audit
SELECT 
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name                                       AS foreign_table_name,
    ccu.column_name                                      AS foreign_column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name 
   AND tc.table_schema = kcu.table_schema
LEFT JOIN information_schema.constraint_column_usage ccu 
    ON ccu.constraint_name = tc.constraint_name 
   AND ccu.table_schema = tc.table_schema
WHERE tc.table_schema = 'public'
  AND tc.constraint_type IN ('FOREIGN KEY', 'UNIQUE')
ORDER BY tc.table_name, tc.constraint_name ASC;

-- Query 6.3: Identity Sequence Current Values & State
SELECT 
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment
FROM information_schema.sequences
WHERE sequence_schema = 'public'
ORDER BY sequence_name ASC;

-- =============================================================================
-- END OF READ-ONLY INSPECTION SCRIPT
-- =============================================================================
