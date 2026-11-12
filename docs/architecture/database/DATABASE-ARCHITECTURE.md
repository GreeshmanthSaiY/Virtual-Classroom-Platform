# Virtual Classroom Platform — Database Architecture & Engineering Design

## 1. Architectural Principles

The Virtual Classroom database architecture is engineered for **relational integrity, scalability, security, and strict consistency**. It is powered by PostgreSQL 14+ / Supabase PostgreSQL, governed by Flyway schema migrations, and accessed via Spring Data JPA with HikariCP connection pooling.

### Core Architectural Tenants:
1. **Third Normal Form (3NF) & Boyce-Codd Normal Form (BCNF) Compliance**:
   - Zero redundant non-key attributes across tables.
   - Separate profile entities for students (`student_profiles`) and faculty (`faculty_profiles`) to eliminate sparse NULL-laden wide tables.
   - Explicit associative junction tables (`student_subject_enrollments`, `faculty_subject_assignments`) supporting composite unique constraints over `(user_id, subject_id, academic_term)`.
2. **Defensive Schema Constraints**:
   - Foreign key constraints on every relationship with deliberate referential actions (`CASCADE`, `RESTRICT`, `SET NULL`).
   - Check constraints (`CHECK`) at the database level enforcing valid roles, statuses, numerical ranges (years 1-6, ratings 1-5, positive points).
   - Unique constraints (`UK`) preventing duplicate natural keys (emails, roll numbers, employee IDs, enrollment pairs, submission pairs).
3. **Optimized Indexing Topology**:
   - B-Tree indexes on all foreign key columns to eliminate table scans during JOIN operations and foreign key cascading.
   - Filtering and state indexes on operational queries (`status`, `is_read`, `scheduled_start`, `due_date`).
4. **Audit Immutability**:
   - Dedicated `audit_logs` table with `ON DELETE SET NULL` on `user_id`, ensuring regulatory audit compliance remains intact even if a user account is purged.

---

## 2. Domain Decomposition

The 20 tables are partitioned into 6 cohesive domains:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       VIRTUAL CLASSROOM DATA DOMAINS                        │
├─────────────────────┬──────────────────────────┬────────────────────────────┤
│ 1. Identity & IAM   │ 2. Academic Catalog      │ 3. Learning Delivery       │
│  - users            │  - courses               │  - study_materials         │
│  - student_profiles │  - subjects              │  - study_material_ratings  │
│  - faculty_profiles │  - student_enrollments   │  - recorded_lectures       │
│  - refresh_tokens   │  - faculty_assignments   │  - live_sessions           │
├─────────────────────┼──────────────────────────┼────────────────────────────┤
│ 4. Evaluation       │ 5. Collaboration         │ 6. Observability           │
│  - assignments      │  - qa_threads            │  - audit_logs              │
│  - submissions      │  - qa_replies            │                            │
│  - examinations     │  - notifications         │                            │
│  - exam_results     │                          │                            │
└─────────────────────┴──────────────────────────┴────────────────────────────┘
```

---

## 3. Comprehensive Indexing Strategy

To guarantee sub-10ms query execution across high-concurrency educational workloads, the database maintains the following index layout:

### Foreign Key & Join Indexes
- `idx_student_profiles_course_id`: Fast lookup of students by degree program.
- `idx_subjects_course_id`: Rapid resolution of curriculum subjects for a degree.
- `idx_enrollments_student_id`: Instant resolution of a student's enrolled courses.
- `idx_enrollments_subject_id`: Fast roster generation for an academic subject.
- `idx_faculty_assignments_subject_id`: Instant identification of assigned faculty.
- `idx_study_materials_subject_id`: Material retrieval for subject syllabus views.
- `idx_recorded_lectures_subject_id`: Video lecture retrieval by subject.
- `idx_live_sessions_subject_id`: Virtual session listings by subject.
- `idx_assignments_subject_id`: Coursework listings by subject.
- `idx_submissions_assignment_id`: Instructor submission inbox retrieval.
- `idx_examinations_subject_id`: Exam schedule lookup by subject.
- `idx_exam_results_examination_id`: Result ledger retrieval for grading.
- `idx_qa_threads_subject_id`: Subject discussion board loading.
- `idx_qa_replies_thread_id`: Thread conversation tree retrieval.

### Operational State & Query Acceleration Indexes
- `idx_users_role_status`: Compound index supporting administrative user review queues (`WHERE role = ? AND status = ?`).
- `idx_live_sessions_status_start`: Compound index for upcoming meeting dashboards (`WHERE status = 'SCHEDULED' ORDER BY scheduled_start ASC`).
- `idx_assignments_due_date`: Upcoming deadline queries for student calendars.
- `idx_submissions_student`: Fast portfolio view of all assignments submitted by a student.
- `idx_exam_results_student`: Rapid student academic transcript generation.
- `idx_notifications_user_unread`: Partial/filtered lookup for user alert counters (`WHERE user_id = ? AND is_read = FALSE`).
- `idx_refresh_tokens_hash`: O(1) hash lookup during token rotation.

---

## 4. Normalization & Integrity Proofs

### Proof of 3NF Compliance
1. **1NF (Atomic Attributes)**: Every column holds atomic, scalar values (strings, integers, timestamps). Multi-valued elements (enrollments, ratings, submissions) reside in dedicated relational child tables.
2. **2NF (Full Functional Dependency)**: All non-key attributes depend on the entire primary key. In compound keys (e.g., `student_subject_enrollments` unique key `(student_id, subject_id, academic_term)`), surrogate `id BIGSERIAL` is the primary key, and non-key attributes (`status`, `enrolled_at`) describe the enrollment instance itself.
3. **3NF (No Transitive Dependencies)**: Non-key attributes depend strictly on candidate keys. For example, `subjects` stores `course_id` rather than course name or degree duration; course details reside exclusively in `courses`. Similarly, student academic metadata resides in `student_profiles`, leaving `users` strictly focused on authentication and principal identity.

---

## 5. Storage & Connection Topology

### Supabase Connection Modes
```
           ┌──────────────────────────────────────────────┐
           │        Virtual Classroom Backend App         │
           │         (Spring Boot 3.4 / HikariCP)         │
           └───────┬──────────────────────────────┬───────┘
                   │                              │
         Port 6543 (Runtime)             Port 5432 (DDL/Admin)
      Transaction Mode Pooling          Session Direct Connection
                   │                              │
                   ▼                              ▼
     ┌────────────────────────────┐ ┌───────────────────────────┐
     │ Supavisor / PgBouncer Pool │ │ PostgreSQL Primary Engine │
     │ (sslmode=require, max: 20) │ │ (Flyway DDL & Migrations) │
     └─────────────┬──────────────┘ └─────────────┬─────────────┘
                   │                              │
                   └──────────────┬───────────────┘
                                  ▼
                   ┌──────────────────────────────┐
                   │   Supabase PostgreSQL 14+   │
                   └──────────────────────────────┘
```

1. **Transaction Pooling (Port 6543)**:
   - Used for application runtime traffic on platforms like Render or AWS ECS.
   - Eliminates connection starvation when scaling web instances.
   - Requires `sslmode=require` and `prepareThreshold=0` in JDBC URL to support transaction-level server-side pooling cleanly.
2. **Direct Connection (Port 5432)**:
   - Used exclusively during Flyway migrations and administrative maintenance.
   - Supports transactional DDL locks (`pg_locks`), prepared statement caches, and schema validation.
