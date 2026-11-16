# Virtual Classroom Platform — Supabase Staging Database Inventory

**Infrastructure Target:** Supabase Staging Database  
**Project Name:** `virtual-classroom-staging`  
**Project Reference:** `zrbpjcfssbtxxgrjtsca`  
**Region:** `ap-south-1` (Mumbai, India)  
**Database Host:** `aws-0-ap-south-1.pooler.supabase.com:5432` (Supavisor Session Pooler)  
**Database:** `postgres`  
**Database Engine:** `PostgreSQL 17.11 on aarch64-unknown-linux-gnu, compiled by gcc (GCC) 15.2.0, 64-bit`  
**Schema:** `public`  
**Audit Timestamp:** 2026-11-16 18:40:38 IST  
**Audit Tool:** `LiveStagingComprehensiveAudit.java` / `StandaloneFlywayMigrationRunner.java`  

---

## 1. Flyway Migration History Inventory

Recorded in `public.flyway_schema_history` on the live staging database:

| Rank | Version | Description | Type | Script | Checksum | Installed By | Execution Time | Success | Installed On (UTC) |
|:---:|:---:|:---|:---:|:---|:---:|:---:|:---:|:---:|:---|
| **1** | `1` | `init schema` | `SQL` | `V1__init_schema.sql` | `-447483902` | `postgres` | 726 ms | **true** | 2026-11-16 13:04:24.280800 |
| **2** | `2` | `seed academic data` | `SQL` | `V2__seed_academic_data.sql` | `652656746` | `postgres` | 529 ms | **true** | 2026-11-16 13:04:25.364436 |
| **3** | `3` | `security harden remove demo users` | `SQL` | `V3__security_harden_remove_demo_users.sql` | `-792140333` | `postgres` | 348 ms | **true** | 2026-11-16 13:04:26.185209 |
| **4** | `4` | `academic platform expansion` | `SQL` | `V4__academic_platform_expansion.sql` | `402235331` | `postgres` | 760 ms | **true** | 2026-11-16 13:04:26.834245 |
| **5** | `5` | `password reset and course archival` | `SQL` | `V5__password_reset_and_course_archival.sql` | `-98890006` | `postgres` | 193 ms | **true** | 2026-11-16 13:04:27.869944 |

- **Total Recorded Migrations:** 5
- **Failed Migrations:** 0
- **Pending Migrations:** 0
- **Total Execution Time:** 2,556 ms (2.56 seconds)
- **Second Invocation Idempotency:** Verified (0 migrations executed, schema version remaining at v5).

---

## 2. Table Inventory (21 Application Tables + 1 Metadata Table)

All 22 tables reside in the `public` schema:

| Table Name | Primary Purpose | Table Type |
|---|---|---|
| `answers` | Community and academic Q&A response records | `BASE TABLE` |
| `assignment_submissions` | Student coursework file and grade submissions | `BASE TABLE` |
| `assignments` | Faculty-issued academic coursework assignments | `BASE TABLE` |
| `audit_logs` | Platform audit trail and security event logs | `BASE TABLE` |
| `courses` | Academic degree programs and specializations | `BASE TABLE` |
| `degrees` | Educational qualification levels (B.Sc, B.Tech, etc.) | `BASE TABLE` |
| `examination_results` | Individual student examination grades & marks | `BASE TABLE` |
| `examinations` | Academic examinations, midterms, finals | `BASE TABLE` |
| `faculty_profiles` | Faculty departmental metadata & employee identifiers | `BASE TABLE` |
| `faculty_subject_assignments` | Faculty teaching allocations per subject & term | `BASE TABLE` |
| `flyway_schema_history` | Flyway canonical migration execution log | `BASE TABLE` |
| `live_sessions` | Real-time virtual classroom lecture sessions | `BASE TABLE` |
| `notifications` | User notification and event alerts | `BASE TABLE` |
| `password_reset_tokens` | Secure SHA-256 tokens for password recovery | `BASE TABLE` |
| `questions` | Student & faculty academic forum inquiries | `BASE TABLE` |
| `refresh_tokens` | JWT refresh tokens with rotation & revocation | `BASE TABLE` |
| `student_profiles` | Student roll numbers, years, and enrolled courses | `BASE TABLE` |
| `student_subject_enrollments` | Semester-level course enrollment tracking | `BASE TABLE` |
| `study_materials` | Uploaded reference documents, PDFs, lecture notes | `BASE TABLE` |
| `subjects` | Curricular subjects, modules, credits, and syllabi | `BASE TABLE` |
| `users` | Platform user accounts, roles, and credentials | `BASE TABLE` |
| `video_lectures` | Recorded video lectures and streaming metadata | `BASE TABLE` |

---

## 3. Comprehensive Column Inventory

### 3.1 Core User & Authentication Subsystem
- **`users`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('users_id_seq')`, PK)
  - `email` (`character varying(255)`, NOT NULL, UNIQUE)
  - `password_hash` (`character varying(255)`, NOT NULL)
  - `full_name` (`character varying(100)`, NOT NULL)
  - `role` (`character varying(20)`, NOT NULL)
  - `status` (`character varying(20)`, NOT NULL, Default: `'PENDING'`)
  - `rejection_reason` (`text`, Nullable)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
  - `updated_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`refresh_tokens`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('refresh_tokens_id_seq')`, PK)
  - `token` (`character varying(255)`, NOT NULL, UNIQUE)
  - `user_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `expiry_date` (`timestamp with time zone`, NOT NULL)
  - `revoked` (`boolean`, NOT NULL, Default: `false`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`password_reset_tokens`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('password_reset_tokens_id_seq')`, PK)
  - `token` (`character varying(255)`, NOT NULL, UNIQUE)
  - `user_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `expiry_date` (`timestamp with time zone`, NOT NULL)
  - `used` (`boolean`, NOT NULL, Default: `false`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)

### 3.2 Academic Curriculum Subsystem
- **`degrees`**
  - `id` (`integer`, NOT NULL, Default: `nextval('degrees_id_seq')`, PK)
  - `code` (`character varying(20)`, NOT NULL, UNIQUE)
  - `name` (`character varying(100)`, NOT NULL)
  - `duration_years` (`integer`, NOT NULL, CHECK: `duration_years BETWEEN 1 AND 5`)
- **`courses`**
  - `id` (`integer`, NOT NULL, Default: `nextval('courses_id_seq')`, PK)
  - `degree_id` (`integer`, NOT NULL, FK -> `degrees(id)`)
  - `code` (`character varying(30)`, NOT NULL, UNIQUE)
  - `name` (`character varying(100)`, NOT NULL)
  - `archived` (`boolean`, NOT NULL, Default: `false`)
- **`subjects`**
  - `id` (`integer`, NOT NULL, Default: `nextval('subjects_id_seq')`, PK)
  - `course_id` (`integer`, NOT NULL, FK -> `courses(id)`)
  - `code` (`character varying(30)`, NOT NULL, UNIQUE)
  - `title` (`character varying(100)`, NOT NULL)
  - `year_level` (`integer`, NOT NULL, CHECK: `year_level BETWEEN 1 AND 5`)
  - `credits` (`integer`, Default: `3`)
  - `semester` (`character varying(20)`, Default: `'SEMESTER_1'`)

### 3.3 Profiles & Academic Allocations
- **`student_profiles`**
  - `user_id` (`bigint`, NOT NULL, PK, FK -> `users(id)`)
  - `roll_number` (`character varying(50)`, NOT NULL, UNIQUE)
  - `course_id` (`integer`, NOT NULL, FK -> `courses(id)`)
  - `current_year` (`integer`, NOT NULL, CHECK: `current_year BETWEEN 1 AND 5`)
  - `avatar_url` (`character varying(500)`, Nullable)
  - `phone_number` (`character varying(20)`, Nullable)
  - `bio` (`text`, Nullable)
- **`faculty_profiles`**
  - `user_id` (`bigint`, NOT NULL, PK, FK -> `users(id)`)
  - `employee_id` (`character varying(50)`, NOT NULL, UNIQUE)
  - `department` (`character varying(100)`, NOT NULL)
  - `avatar_url` (`character varying(500)`, Nullable)
  - `phone_number` (`character varying(20)`, Nullable)
  - `bio` (`text`, Nullable)
- **`faculty_subject_assignments`**
  - `faculty_id` (`bigint`, NOT NULL, FK -> `faculty_profiles(user_id)`)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `academic_term` (`character varying(20)`, NOT NULL, Default: `'2026-ACTIVE'`)
  - `assigned_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
  - PK: (`faculty_id`, `subject_id`, `academic_term`)
- **`student_subject_enrollments`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('student_subject_enrollments_id_seq')`, PK)
  - `student_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `academic_term` (`character varying(20)`, NOT NULL)
  - `enrollment_status` (`character varying(20)`, NOT NULL, Default: `'ENROLLED'`)
  - `enrolled_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)

### 3.4 Content & Engagement Subsystem
- **`study_materials`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('study_materials_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `uploader_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `description` (`text`, Nullable)
  - `file_url` (`character varying(500)`, NOT NULL)
  - `file_size_bytes` (`bigint`, NOT NULL, Default: `0`)
  - `mime_type` (`character varying(100)`, NOT NULL, Default: `'application/pdf'`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`video_lectures`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('video_lectures_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `faculty_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `description` (`text`, Nullable)
  - `video_url` (`character varying(500)`, NOT NULL)
  - `duration_seconds` (`integer`, Default: `0`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`live_sessions`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('live_sessions_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `faculty_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `description` (`text`, Nullable)
  - `meeting_link` (`character varying(500)`, NOT NULL)
  - `scheduled_start_time` (`timestamp with time zone`, NOT NULL)
  - `scheduled_end_time` (`timestamp with time zone`, NOT NULL)
  - `status` (`character varying(20)`, NOT NULL, Default: `'SCHEDULED'`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`questions`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('questions_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `author_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(250)`, NOT NULL)
  - `body` (`text`, NOT NULL)
  - `is_resolved` (`boolean`, NOT NULL, Default: `false`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
  - `updated_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`answers`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('answers_id_seq')`, PK)
  - `question_id` (`bigint`, NOT NULL, FK -> `questions(id)`)
  - `author_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `body` (`text`, NOT NULL)
  - `is_accepted` (`boolean`, NOT NULL, Default: `false`)
  - `is_endorsed` (`boolean`, NOT NULL, Default: `false`)
  - `endorsed_by` (`bigint`, Nullable, FK -> `users(id)`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
  - `updated_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)

### 3.5 Assessment & Evaluation Subsystem
- **`assignments`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('assignments_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `faculty_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `description` (`text`, Nullable)
  - `due_date` (`timestamp with time zone`, NOT NULL)
  - `max_marks` (`integer`, NOT NULL, Default: `100`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`assignment_submissions`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('assignment_submissions_id_seq')`, PK)
  - `assignment_id` (`bigint`, NOT NULL, FK -> `assignments(id)`)
  - `student_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `file_url` (`character varying(500)`, NOT NULL)
  - `submission_text` (`text`, Nullable)
  - `obtained_marks` (`integer`, Nullable)
  - `feedback` (`text`, Nullable)
  - `status` (`character varying(20)`, NOT NULL, Default: `'SUBMITTED'`)
  - `submitted_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
  - `graded_at` (`timestamp with time zone`, Nullable)
- **`examinations`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('examinations_id_seq')`, PK)
  - `subject_id` (`integer`, NOT NULL, FK -> `subjects(id)`)
  - `faculty_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `exam_type` (`character varying(20)`, NOT NULL, Default: `'MIDTERM'`)
  - `exam_date` (`timestamp with time zone`, NOT NULL)
  - `duration_minutes` (`integer`, NOT NULL, Default: `60`)
  - `max_marks` (`integer`, NOT NULL, Default: `100`)
  - `instructions` (`text`, Nullable)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`examination_results`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('examination_results_id_seq')`, PK)
  - `examination_id` (`bigint`, NOT NULL, FK -> `examinations(id)`)
  - `student_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `marks_obtained` (`numeric(5,2)`, NOT NULL)
  - `grade` (`character varying(5)`, Nullable)
  - `remarks` (`text`, Nullable)
  - `graded_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)

### 3.6 Communication & Platform Audit
- **`notifications`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('notifications_id_seq')`, PK)
  - `user_id` (`bigint`, NOT NULL, FK -> `users(id)`)
  - `title` (`character varying(200)`, NOT NULL)
  - `message` (`text`, NOT NULL)
  - `is_read` (`boolean`, NOT NULL, Default: `false`)
  - `created_at` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)
- **`audit_logs`**
  - `id` (`bigint`, NOT NULL, Default: `nextval('audit_logs_id_seq')`, PK)
  - `user_id` (`bigint`, Nullable, FK -> `users(id)`)
  - `action` (`character varying(100)`, NOT NULL)
  - `details` (`text`, Nullable)
  - `ip_address` (`character varying(45)`, Nullable)
  - `timestamp` (`timestamp with time zone`, Default: `CURRENT_TIMESTAMP`)

---

## 4. Custom Index Inventory (25 Indexes)

| # | Index Name | Table | Indexed Columns | Index Type |
|:---:|:---|:---|:---|:---:|
| 1 | `idx_users_role_status` | `users` | `(role, status)` | `btree` |
| 2 | `idx_users_email` | `users` | `(email)` | `btree` |
| 3 | `idx_courses_degree` | `courses` | `(degree_id)` | `btree` |
| 4 | `idx_courses_archived` | `courses` | `(archived)` | `btree` |
| 5 | `idx_subjects_course_year` | `subjects` | `(course_id, year_level)` | `btree` |
| 6 | `idx_materials_subject` | `study_materials` | `(subject_id)` | `btree` |
| 7 | `idx_lectures_subject` | `video_lectures` | `(subject_id)` | `btree` |
| 8 | `idx_questions_subject` | `questions` | `(subject_id)` | `btree` |
| 9 | `idx_questions_author` | `questions` | `(author_id)` | `btree` |
| 10 | `idx_answers_question` | `answers` | `(question_id)` | `btree` |
| 11 | `idx_answers_author` | `answers` | `(author_id)` | `btree` |
| 12 | `idx_answers_endorsed` | `answers` | `(is_endorsed)` | `btree` |
| 13 | `idx_notifications_user` | `notifications` | `(user_id)` | `btree` |
| 14 | `idx_audit_logs_user` | `audit_logs` | `(user_id)` | `btree` |
| 15 | `idx_refresh_tokens_token` | `refresh_tokens` | `(token)` | `btree` |
| 16 | `idx_refresh_tokens_user` | `refresh_tokens` | `(user_id)` | `btree` |
| 17 | `idx_student_enrollments_student` | `student_subject_enrollments` | `(student_id)` | `btree` |
| 18 | `idx_student_enrollments_subject` | `student_subject_enrollments` | `(subject_id)` | `btree` |
| 19 | `idx_live_sessions_subject` | `live_sessions` | `(subject_id)` | `btree` |
| 20 | `idx_assignments_subject` | `assignments` | `(subject_id)` | `btree` |
| 21 | `idx_assignment_submissions_assign` | `assignment_submissions` | `(assignment_id)` | `btree` |
| 22 | `idx_assignment_submissions_student` | `assignment_submissions` | `(student_id)` | `btree` |
| 23 | `idx_examinations_subject` | `examinations` | `(subject_id)` | `btree` |
| 24 | `idx_examination_results_exam` | `examination_results` | `(examination_id)` | `btree` |
| 25 | `idx_password_reset_token` | `password_reset_tokens` | `(token)` | `btree` |

---

## 5. Sequence Inventory (18 Sequences)

| # | Sequence Name | Owner Table | Owner Column | Data Type | Start Value | Last Value | Status |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---|
| 1 | `users_id_seq` | `users` | `id` | `bigint` | 1 | `null` | **Cleanly reset (Uncalled)** |
| 2 | `questions_id_seq` | `questions` | `id` | `bigint` | 1 | `null` | **Cleanly reset (Uncalled)** |
| 3 | `degrees_id_seq` | `degrees` | `id` | `integer` | 1 | `5` | Active (5 degrees seeded) |
| 4 | `courses_id_seq` | `courses` | `id` | `integer` | 1 | `9` | Active (9 courses seeded) |
| 5 | `subjects_id_seq` | `subjects` | `id` | `integer` | 1 | `8` | Active (8 subjects seeded) |
| 6 | `study_materials_id_seq` | `study_materials` | `id` | `bigint` | 1 | `2` | Ready (Demo cleaned) |
| 7 | `video_lectures_id_seq` | `video_lectures` | `id` | `bigint` | 1 | `2` | Ready (Demo cleaned) |
| 8 | `answers_id_seq` | `answers` | `id` | `bigint` | 1 | `1` | Ready (Demo cleaned) |
| 9 | `notifications_id_seq` | `notifications` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 10 | `audit_logs_id_seq` | `audit_logs` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 11 | `refresh_tokens_id_seq` | `refresh_tokens` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 12 | `student_subject_enrollments_id_seq` | `student_subject_enrollments` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 13 | `live_sessions_id_seq` | `live_sessions` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 14 | `assignments_id_seq` | `assignments` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 15 | `assignment_submissions_id_seq` | `assignment_submissions` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 16 | `examinations_id_seq` | `examinations` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 17 | `examination_results_id_seq` | `examination_results` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |
| 18 | `password_reset_tokens_id_seq` | `password_reset_tokens` | `id` | `bigint` | 1 | `null` | Ready (Uncalled) |

---

## 6. Canonical Academic Reference Data

### 6.1 Degrees (5 Records)
```
ID 1: B.Sc       — Bachelor of Science                   (Duration: 3 years)
ID 2: M.Sc       — Master of Science                     (Duration: 2 years)
ID 3: B.Tech     — Bachelor of Technology                (Duration: 4 years)
ID 4: B.A.       — Bachelor of Arts                      (Duration: 3 years)
ID 5: M.A.       — Master of Arts                        (Duration: 2 years)
```

### 6.2 Courses (9 Records)
```
ID 1: BSC-CS     — B.Sc Computer Science                 (Degree: B.Sc,   Archived: false)
ID 2: BSC-DS     — B.Sc Data Science                     (Degree: B.Sc,   Archived: false)
ID 3: MSC-AI     — M.Sc Artificial Intelligence          (Degree: M.Sc,   Archived: false)
ID 4: BTECH-CS   — B.Tech Computer Science & Engineering (Degree: B.Tech, Archived: false)
ID 5: BTECH-IT   — B.Tech Information Technology         (Degree: B.Tech, Archived: false)
ID 6: BTECH-ECE  — B.Tech Electronics & Communication    (Degree: B.Tech, Archived: false)
ID 7: BA-ENG     — B.A. English Literature               (Degree: B.A.,   Archived: false)
ID 8: BA-HIST    — B.A. History                          (Degree: B.A.,   Archived: false)
ID 9: MA-ENG     — M.A. English Literature               (Degree: M.A.,   Archived: false)
```

### 6.3 Subjects (8 Records)
```
ID 1: CS101      — Programming Fundamentals (Course: BSC-CS,   Year: 1, Sem: SEMESTER_1, Credits: 4)
ID 2: CS102      — Data Structures          (Course: BSC-CS,   Year: 1, Sem: SEMESTER_1, Credits: 4)
ID 3: DS101      — Introduction to Data Sci (Course: BSC-DS,   Year: 1, Sem: SEMESTER_1, Credits: 3)
ID 4: AI501      — Machine Learning Core    (Course: MSC-AI,   Year: 1, Sem: SEMESTER_1, Credits: 4)
ID 5: BTCS101    — Engineering Math I       (Course: BTECH-CS, Year: 1, Sem: SEMESTER_1, Credits: 4)
ID 6: BTCS102    — Computer Organization    (Course: BTECH-CS, Year: 1, Sem: SEMESTER_1, Credits: 4)
ID 7: ENG101     — British Literature       (Course: BA-ENG,   Year: 1, Sem: SEMESTER_1, Credits: 3)
ID 8: HIST101    — Ancient World History    (Course: BA-HIST,  Year: 1, Sem: SEMESTER_1, Credits: 3)
```

---

## 7. Row-Level Security (RLS) & Storage

### 7.1 Table RLS Status
- **RLS Enabled:** `true` on all 22 tables in `public` (enforced by Supabase's `ensure_rls` event trigger).
- **RLS Forced:** `false` across all tables.
- **Custom PostgREST Policies:** `0` (Zero public API exposure).
- **Backend Session Role (`postgres`):** Has `rolbypassrls = true`. Spring Boot queries run unhindered via JDBC while protecting tables from unauthenticated Direct HTTP/PostgREST queries.

### 7.2 Storage Bucket (`storage.buckets`)
- **Bucket ID:** `academic-assets`
- **Bucket Name:** `academic-assets`
- **Public Access:** `false` (Private, signed URLs required)
- **File Size Limit:** `52428800` bytes (50 MB)
- **Allowed MIME Types:** 14 strictly allowed document, image, and video formats.
