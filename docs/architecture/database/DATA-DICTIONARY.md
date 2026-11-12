# Virtual Classroom Platform — Data Dictionary

## Overview

This Data Dictionary defines the schema specification for all **20 production tables** across the Virtual Classroom Platform, spanning Flyway migrations `V1__init_schema.sql` through `V4__academic_platform_expansion.sql`.

---

## 1. Domain: Identity & Access Management (IAM)

### 1.1 `users`
Core authentication and user authorization account entity.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Unique internal surrogate identifier. |
| `email` | `VARCHAR(150)` | No | None | `UK`, Lowercase | Unique primary login email address. |
| `password_hash` | `VARCHAR(255)` | No | None | BCrypt (cost 12) | Cryptographic salt + hash of user password. |
| `full_name` | `VARCHAR(120)` | No | None | Trimmed | Legal or academic full name of user. |
| `role` | `VARCHAR(30)` | No | None | `CHECK (role IN ('ROLE_STUDENT','ROLE_FACULTY','ROLE_ADMIN'))` | System RBAC authorization role. |
| `status` | `VARCHAR(30)` | No | None | `CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED','DISABLED'))` | Administrative lifecycle review state. |
| `rejection_reason` | `VARCHAR(255)` | Yes | NULL | Optional text | Documented reason if administrator rejected approval. |
| `avatar_url` | `VARCHAR(512)` | Yes | NULL | Valid URL/URI | Storage URL for profile picture (Supabase bucket). |
| `phone_number` | `VARCHAR(25)` | Yes | NULL | E.164 format | Contact telephone number. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Registration timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Last profile update timestamp. |

### 1.2 `student_profiles`
Extended profile for users with `ROLE_STUDENT`.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `user_id` | `BIGINT` | No | None | `PK`, `FK -> users(id) ON DELETE CASCADE` | 1:1 foreign key binding to parent user. |
| `roll_number` | `VARCHAR(50)` | No | None | `UK` | University institutional student matriculation roll. |
| `course_id` | `INTEGER` | No | None | `FK -> courses(id) ON DELETE RESTRICT` | Primary enrolled degree curriculum. |
| `current_year` | `INTEGER` | No | None | `CHECK (current_year BETWEEN 1 AND 6)` | Academic study year level (e.g. 1st, 2nd, 3rd, 4th). |
| `bio` | `TEXT` | Yes | NULL | Max 1000 chars | Student academic interests and short bio. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Record creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Profile modification timestamp. |

### 1.3 `faculty_profiles`
Extended profile for users with `ROLE_FACULTY`.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `user_id` | `BIGINT` | No | None | `PK`, `FK -> users(id) ON DELETE CASCADE` | 1:1 foreign key binding to parent user. |
| `employee_id` | `VARCHAR(50)` | No | None | `UK` | Institutional staff/faculty employee ID number. |
| `department` | `VARCHAR(100)` | No | None | Text | Academic department (e.g. "Computer Science"). |
| `designation` | `VARCHAR(100)` | Yes | NULL | Text | Academic title (e.g. "Associate Professor", "Dean"). |
| `office_hours` | `VARCHAR(255)` | Yes | NULL | Text | Weekly office consultation schedule availability. |
| `bio` | `TEXT` | Yes | NULL | Max 2000 chars | Research background and pedagogical biography. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Record creation timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Profile modification timestamp. |

### 1.4 `refresh_tokens`
Opaque refresh tokens for cryptographic session rotation.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Primary surrogate key. |
| `user_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE` | Associated user identity. |
| `token_hash` | `VARCHAR(255)` | No | None | `UK` | Cryptographic hash of client-supplied refresh token. |
| `expires_at` | `TIMESTAMPTZ` | No | None | Future timestamp | Expiration deadline for token renewal. |
| `revoked` | `BOOLEAN` | No | `FALSE` | Indexed | Flag indicating if rotated or manually logged out. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Generation timestamp. |

---

## 2. Domain: Academic Catalog & Enrollment

### 2.1 `courses`
Academic degree programs (e.g. B.Tech Computer Science).

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `SERIAL` | No | Auto | `PK` | Degree identifier. |
| `code` | `VARCHAR(20)` | No | None | `UK` | Unique program code (e.g., "BTECH-CS"). |
| `name` | `VARCHAR(120)` | No | None | Text | Full title of academic qualification. |
| `duration_years` | `INTEGER` | No | None | `CHECK (duration_years BETWEEN 1 AND 6)` | Standard graduation duration in academic years. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Catalog entry timestamp. |

### 2.2 `subjects`
Curriculum modules and academic courses taught within degrees.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `SERIAL` | No | Auto | `PK` | Unique subject identifier. |
| `course_id` | `INTEGER` | No | None | `FK -> courses(id) ON DELETE RESTRICT` | Degree curriculum this subject belongs to. |
| `code` | `VARCHAR(30)` | No | None | `UK` | Standard university course code (e.g., "CS-301"). |
| `title` | `VARCHAR(100)` | No | None | Text | Official subject title. |
| `year_level` | `INTEGER` | No | None | `CHECK (year_level BETWEEN 1 AND 6)` | Prescribed year of study. |
| `semester` | `INTEGER` | No | `1` | `CHECK (semester BETWEEN 1 AND 12)` | Prescribed semester within curriculum. |
| `credits` | `INTEGER` | No | `3` | `CHECK (credits BETWEEN 1 AND 12)` | Academic credit unit rating. |
| `description` | `TEXT` | Yes | NULL | Text | Syllabus description and objectives. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Syllabus entry timestamp. |

### 2.3 `student_subject_enrollments`
Formal registration of a student in a specific subject for an academic term.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Enrollment record surrogate key. |
| `student_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE` | Enrolled student user. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Enrolled subject syllabus. |
| `academic_term` | `VARCHAR(30)` | No | `'2026-ACTIVE'` | `UK(student, subject, term)` | Academic term (e.g. "2026-FALL"). |
| `status` | `VARCHAR(20)` | No | `'ENROLLED'` | `CHECK (status IN ('ENROLLED','COMPLETED','DROPPED'))` | Course enrollment status. |
| `enrolled_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Registration timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Status modification timestamp. |

### 2.4 `faculty_subject_assignments`
Assignment of teaching responsibility to faculty members.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Teaching assignment surrogate key. |
| `faculty_id` | `BIGINT` | No | None | `FK -> faculty_profiles(user_id) ON DELETE CASCADE` | Assigned faculty member. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Subject assigned to teach. |
| `academic_term` | `VARCHAR(30)` | No | None | `UK(faculty, subject, term)` | Academic term. |
| `assigned_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Assignment assignment timestamp. |

---

## 3. Domain: Learning Resources & Delivery

### 3.1 `study_materials`
Academic reading material, PDFs, lecture slides, and notes.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Resource surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Associated subject. |
| `faculty_id` | `BIGINT` | No | None | `FK -> faculty_profiles(user_id) ON DELETE RESTRICT` | Uploading faculty author. |
| `title` | `VARCHAR(150)` | No | None | Text | Human-readable title of document. |
| `description` | `TEXT` | Yes | NULL | Text | Summary of document contents. |
| `file_name` | `VARCHAR(255)` | No | None | Text | Original uploaded file name. |
| `file_path` | `VARCHAR(500)` | No | None | URL/Path | Supabase Storage path (`materials` bucket). |
| `content_type` | `VARCHAR(100)` | No | None | MIME type | MIME type (e.g. `application/pdf`). |
| `file_size_bytes` | `BIGINT` | No | None | Positive | File size in bytes (max 50 MB). |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Upload timestamp. |

### 3.2 `study_material_ratings`
Student feedback and quality evaluation ratings for study materials.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Rating surrogate key. |
| `material_id` | `BIGINT` | No | None | `FK -> study_materials(id) ON DELETE CASCADE` | Rated document. |
| `student_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE`, `UK(mat, stu)` | Student reviewer. |
| `rating` | `INTEGER` | No | None | `CHECK (rating BETWEEN 1 AND 5)` | 1-to-5 star quality rating. |
| `review_text` | `TEXT` | Yes | NULL | Optional text | Constructive written feedback. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Review submission timestamp. |

### 3.3 `recorded_lectures`
Archived video lectures and audio recordings.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Video lecture surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Subject curriculum. |
| `faculty_id` | `BIGINT` | No | None | `FK -> faculty_profiles(user_id) ON DELETE RESTRICT` | Lecturer / presenter. |
| `title` | `VARCHAR(150)` | No | None | Text | Title of lecture topic. |
| `description` | `TEXT` | Yes | NULL | Text | Lecture notes, outline, and links. |
| `video_url` | `VARCHAR(500)` | No | None | Valid URI | Secure streaming / CDN video URL. |
| `duration_seconds` | `INTEGER` | Yes | NULL | Positive | Length of video playback in seconds. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Publishing timestamp. |

### 3.4 `live_sessions`
Interactive synchronous virtual classroom meetings.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Session surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Academic subject. |
| `host_faculty_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE RESTRICT` | Faculty host and moderator. |
| `title` | `VARCHAR(200)` | No | None | Text | Session topic title. |
| `description` | `TEXT` | Yes | NULL | Text | Session agenda and preparation. |
| `meeting_link` | `VARCHAR(500)` | No | None | URL | Video conferencing link (Jitsi, WebRTC). |
| `platform` | `VARCHAR(50)` | No | `'JITSI_MEET'` | Text | Conferencing engine. |
| `scheduled_start` | `TIMESTAMPTZ` | No | None | Timestamp | Scheduled start time. |
| `scheduled_end` | `TIMESTAMPTZ` | No | None | `CHECK (end > start)` | Scheduled conclusion time. |
| `status` | `VARCHAR(30)` | No | `'SCHEDULED'` | `CHECK (status IN ('SCHEDULED','LIVE','COMPLETED','CANCELLED'))` | Live session status. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Scheduling timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Modification timestamp. |

---

## 4. Domain: Coursework & Evaluation

### 4.1 `assignments`
Coursework projects, homework, and problem sets.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Assignment surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Associated subject. |
| `created_by` | `BIGINT` | No | None | `FK -> users(id) ON DELETE RESTRICT` | Faculty author. |
| `title` | `VARCHAR(200)` | No | None | Text | Assignment title. |
| `description` | `TEXT` | No | None | Text | Detailed instructions and rubrics. |
| `due_date` | `TIMESTAMPTZ` | No | None | Timestamp | Submission deadline. |
| `max_points` | `INTEGER` | No | `100` | `CHECK (max_points > 0)` | Maximum obtainable score points. |
| `attachment_url` | `VARCHAR(500)` | Yes | NULL | URL | Reference worksheet / template file. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Publishing timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Revision timestamp. |

### 4.2 `assignment_submissions`
Student coursework submissions and grading feedback.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Submission surrogate key. |
| `assignment_id` | `BIGINT` | No | None | `FK -> assignments(id) ON DELETE CASCADE` | Target assignment. |
| `student_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE`, `UK(asgn, stu)` | Submitting student. |
| `submission_text`| `TEXT` | Yes | NULL | Text | Written response or GitHub repository URL. |
| `file_url` | `VARCHAR(500)` | Yes | NULL | URL | Uploaded PDF/ZIP file (Supabase `submissions`). |
| `submitted_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Hand-in timestamp. |
| `grade_points` | `INTEGER` | Yes | NULL | `CHECK (grade_points >= 0)` | Awarded marks. |
| `feedback` | `TEXT` | Yes | NULL | Text | Pedagogical comments from instructor. |
| `graded_by` | `BIGINT` | Yes | NULL | `FK -> users(id) ON DELETE SET NULL` | Faculty grader. |
| `graded_at` | `TIMESTAMPTZ` | Yes | NULL | Timestamp | Grading completion timestamp. |
| `status` | `VARCHAR(30)` | No | `'SUBMITTED'` | `CHECK (status IN ('DRAFT','SUBMITTED','LATE','GRADED','RESUBMISSION_REQUESTED'))` | Assessment workflow state. |

### 4.3 `examinations`
Formal academic midterm, final, and quiz examinations.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Examination surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Academic subject. |
| `created_by` | `BIGINT` | No | None | `FK -> users(id) ON DELETE RESTRICT` | Faculty examiner. |
| `title` | `VARCHAR(200)` | No | None | Text | Examination title. |
| `exam_type` | `VARCHAR(50)` | No | None | `CHECK (exam_type IN ('QUIZ','MIDTERM','FINAL','LAB_PRACTICAL'))` | Assessment category. |
| `total_marks` | `INTEGER` | No | None | `CHECK (total_marks > 0)` | Maximum achievable marks. |
| `duration_minutes`| `INTEGER` | No | None | `CHECK (duration_minutes > 0)` | Allocated exam time limit. |
| `scheduled_at` | `TIMESTAMPTZ` | No | None | Timestamp | Exam commencement date and time. |
| `instructions` | `TEXT` | Yes | NULL | Text | Regulations, permissible tools, instructions. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Scheduling timestamp. |

### 4.4 `examination_results`
Certified grades and transcript marks for examinations.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Result surrogate key. |
| `examination_id`| `BIGINT` | No | None | `FK -> examinations(id) ON DELETE CASCADE` | Associated examination. |
| `student_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE`, `UK(exam, stu)` | Evaluated student. |
| `marks_obtained`| `NUMERIC(5,2)` | No | None | `CHECK (marks_obtained >= 0)` | Exact marks achieved. |
| `grade` | `VARCHAR(5)` | Yes | NULL | Text | Letter grade (e.g. "A+", "B", "F"). |
| `remarks` | `TEXT` | Yes | NULL | Text | Evaluator commentary and review notes. |
| `graded_by` | `BIGINT` | Yes | NULL | `FK -> users(id) ON DELETE SET NULL` | Certifying faculty member. |
| `graded_at` | `TIMESTAMPTZ` | Yes | NULL | Timestamp | Certification timestamp. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Ledger entry timestamp. |

---

## 5. Domain: Collaboration & Communication

### 5.1 `qa_threads`
Public discussion forum threads tied to academic subjects.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Thread surrogate key. |
| `subject_id` | `INTEGER` | No | None | `FK -> subjects(id) ON DELETE CASCADE` | Associated subject. |
| `author_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE` | Question author. |
| `title` | `VARCHAR(200)` | No | None | Text | Question title summary. |
| `content` | `TEXT` | No | None | Text | Detailed inquiry or discussion topic. |
| `upvotes` | `INTEGER` | No | `0` | `CHECK (upvotes >= 0)` | Community score. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Publication timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Modification timestamp. |

### 5.2 `qa_replies`
Responses and answers posted in Q&A discussion threads.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Reply surrogate key. |
| `thread_id` | `BIGINT` | No | None | `FK -> qa_threads(id) ON DELETE CASCADE` | Parent discussion thread. |
| `author_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE` | Reply author. |
| `content` | `TEXT` | No | None | Text | Answer or commentary text. |
| `is_endorsed` | `BOOLEAN` | No | `FALSE` | Boolean | True if faculty verified answer as authoritative. |
| `upvotes` | `INTEGER` | No | `0` | `CHECK (upvotes >= 0)` | Community upvote count. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Publication timestamp. |
| `updated_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Modification timestamp. |

### 5.3 `notifications`
Targeted alerts, deadlines, and system events for users.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Notification surrogate key. |
| `user_id` | `BIGINT` | No | None | `FK -> users(id) ON DELETE CASCADE` | Recipient user. |
| `title` | `VARCHAR(150)` | No | None | Text | Short subject line. |
| `message` | `TEXT` | No | None | Text | Complete notification body text. |
| `category` | `VARCHAR(50)` | No | `'GENERAL'` | Text | Event category (e.g. `ASSIGNMENT`, `LIVE_SESSION`). |
| `is_read` | `BOOLEAN` | No | `FALSE` | Indexed | Delivery / acknowledgment state. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Dispatch timestamp. |

---

## 6. Domain: Observability & Auditing

### 6.1 `audit_logs`
Immutable compliance and security activity ledger.

| Column | Type | Nullable | Default | Constraints | Description |
|:---|:---|:---|:---|:---|:---|
| `id` | `BIGSERIAL` | No | Auto | `PK` | Audit event surrogate key. |
| `user_id` | `BIGINT` | Yes | NULL | `FK -> users(id) ON DELETE SET NULL` | Actor initiating operation (null if system). |
| `action` | `VARCHAR(80)` | No | None | Text | Security action code (e.g. `USER_APPROVED`). |
| `target_entity` | `VARCHAR(80)` | No | None | Text | Affected domain entity (e.g. `User`, `Assignment`). |
| `target_id` | `VARCHAR(80)` | Yes | NULL | Text | Primary key of target entity. |
| `details` | `TEXT` | Yes | NULL | Text/JSON | Descriptive metadata and payload changes. |
| `ip_address` | `VARCHAR(45)` | Yes | NULL | IPv4 / IPv6 | Network origin address of request. |
| `created_at` | `TIMESTAMPTZ` | No | `CURRENT_TIMESTAMP` | System managed | Immutable chronological timestamp. |
