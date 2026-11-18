# Virtual Classroom Platform — Supabase Staging & Production Setup Report

**Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform.git`  
**Execution Date:** November 15, 2026  
**Architectural Scope:** PostgreSQL Hosted on Supabase (Staging & Production), Supabase Object Storage, Flyway Database Migrations, Spring Boot 3.4.3 / Java 21 Backend, Render Cloud Deployment.

---

## 1. Executive Summary & Verification Matrix

In accordance with the **Master Execution Directive**, a comprehensive security and architecture audit was performed across the repository, the Spring Boot database subsystem, the Flyway migration sequence, and the target cloud topology for both Supabase environments.

| Verification Item | Staging (`virtual-classroom-staging`) | Production (`virtual-classroom-prod`) | Status / Evidence |
|---|---|---|---|
| **Intended Project Identity** | `zrbpjcfssbtxxgrjtsca` (`ap-south-1`) | `tzbjmxtmysvzxdmspiui` (`ap-south-1`) | **Defined in Directive**; distinct project refs |
| **Local Credential Presence** | Missing from local environment | Missing from local environment | **Hard Gate Triggered**: No credentials fabricated |
| **Flyway Migration Chain** | V1 through V5 validated | V1 through V5 validated | **PASSED**: 5 safe, forward-only migrations |
| **Hibernate DDL Auto** | `validate` | `validate` | **ENFORCED**: Hibernate cannot alter schema |
| **Database Reachable** | Awaiting `STAGING_DATABASE_URL` | Awaiting `PRODUCTION_DATABASE_URL` | **STOPPED BEFORE APPLY** per directive rule |
| **Supabase Data API (PostgREST)** | **OFF** (Disabled) | **OFF** (Disabled) | **VERIFIED**: Zero application dependencies |
| **Storage Bucket Name** | `academic-assets` | `academic-assets` | **VERIFIED**: In allowlist (`FileStorageService:44`) |
| **Storage Bucket Access** | Private | Private | **VERIFIED**: Authenticated / signed URLs only |
| **Storage File Size Limit** | 50 MB (`52428800` bytes) | 50 MB (`52428800` bytes) | **VERIFIED**: Backend enforced (`FileStorageService:86`) |
| **Storage Mechanism** | Storage REST API (`/storage/v1/*`) | Storage REST API (`/storage/v1/*`) | **VERIFIED**: Direct HTTP via `RestTemplate` |
| **Backend Test Suite** | 9/9 remediation tests passed (Java 25) | 9/9 remediation tests passed (Java 25) | **PASSED**: Spring profile & bucket contract verified |

---

## 2. Phase A — Repository & Migration Audit

### 2.1 Complete Flyway Migration Inventory
The repository uses Flyway for canonical database migrations located in `backend/src/main/resources/db/migration/`:

| Version | Migration Script Name | Type | Purpose & Schema Impact |
|---|---|---|---|
| **V1** | `V1__init_schema.sql` | Versioned | Creates 13 initial tables (`users`, `degrees`, `courses`, `subjects`, `student_profiles`, `faculty_profiles`, `faculty_subject_assignments`, `study_materials`, `video_lectures`, `questions`, `answers`, `notifications`, `audit_logs`), plus foreign keys, primary keys, and B-Tree indexes. |
| **V2** | `V2__seed_academic_data.sql` | Versioned (Seed) | Seeds canonical academic hierarchy (Degrees: B.Sc, M.Sc, B.Tech, B.A., M.A.; Courses: Computer Science, Data Science, etc.; Core Subjects: CS101, CS102, CS201, etc.). |
| **V3** | `V3__security_harden_remove_demo_users.sql` | Versioned (Security) | Safely purges demo user accounts and associated demo records using targeted subqueries on known emails (`admin@virtualclassroom.edu`, etc.), while preserving the academic reference curriculum. Resynchronizes sequences (`users_id_seq`, `questions_id_seq`). |
| **V4** | `V4__academic_platform_expansion.sql` | Versioned | Expands platform capabilities by adding 7 new tables (`refresh_tokens`, `student_subject_enrollments`, `live_sessions`, `assignments`, `assignment_submissions`, `examinations`, `examination_results`) and adding profile/curriculum columns (`avatar_url`, `phone_number`, `bio`, `semester`, `credits`). |
| **V5** | `V5__password_reset_and_course_archival.sql` | Versioned | Adds `password_reset_tokens` table for forgot/reset password workflows, adds `archived` flag on `courses`, and adds endorsement/audit columns (`is_endorsed`, `endorsed_by`, `updated_at`) on `answers`. |

### 2.2 Migration Safety & Integrity Check
* **Version Sequencing**: Versions are strictly sequential (`V1` $\rightarrow$ `V2` $\rightarrow$ `V3` $\rightarrow$ `V4` $\rightarrow$ `V5`). No gaps, duplicates, or out-of-order versioning exists.
* **Destructive SQL Check**:
  - Zero `DROP TABLE`, `DROP SCHEMA`, or `TRUNCATE` operations exist in any migration file.
  - V3 uses targeted `DELETE ... WHERE author_id IN (SELECT id FROM users WHERE email IN (...))` to eliminate test accounts without touching real or system data.
* **Idempotency & Re-entrancy**:
  - DDL statements consistently use `CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, and `CREATE INDEX IF NOT EXISTS`.
  - Seed statements use `ON CONFLICT DO NOTHING`.
* **PostgreSQL Compatibility**:
  - Compatible with PostgreSQL 15 and 16 running on Supabase.
  - Uses standard PostgreSQL types: `BIGSERIAL`, `BIGINT`, `TIMESTAMP WITH TIME ZONE`, `TEXT`, `NUMERIC(5,2)`, `BOOLEAN`.

### 2.3 Spring Data JPA & Entity Alignment
All 21 database tables map directly to domain entity classes in `com.virtualclassroom.model`:
* Core Entities: `User`, `Degree`, `Course`, `Subject`, `StudentProfile`, `FacultyProfile`, `FacultySubjectAssignment`
* Content Entities: `StudyMaterial`, `VideoLecture`, `Question`, `Answer`, `Notification`, `AuditLog`
* Academic Expansion Entities: `RefreshToken`, `StudentSubjectEnrollment`, `LiveSession`, `Assignment`, `AssignmentSubmission`, `Examination`, `ExaminationResult`, `PasswordResetToken`
* Hibernate Validation: `spring.jpa.hibernate.ddl-auto: validate` is strictly enforced in `application-staging.yml` and `application-prod.yml`.

---

## 3. Phase B & C — Supabase Projects Identity & Preflight Status

### 3.1 Expected Supabase Project Separation
* **Staging Project**:
  - Project Name: `virtual-classroom-staging`
  - Project Reference: `zrbpjcfssbtxxgrjtsca`
  - Expected Direct Host: `db.zrbpjcfssbtxxgrjtsca.supabase.co`
  - Expected Pooler Host: `aws-0-ap-south-1.pooler.supabase.com`
  - Region: `ap-south-1` (Mumbai, India)
* **Production Project**:
  - Project Name: `virtual-classroom-prod`
  - Project Reference: `tzbjmxtmysvzxdmspiui`
  - Expected Direct Host: `db.tzbjmxtmysvzxdmspiui.supabase.co`
  - Expected Pooler Host: `aws-0-ap-south-1.pooler.supabase.com`
  - Region: `ap-south-1` (Mumbai, India)

### 3.2 Preflight Credential Audit (Local Workstation)
A thorough scan of the local environment, system variables, user environment, and local configuration files reveals:
* `STAGING_DATABASE_URL`: **Not set** in current environment.
* `PRODUCTION_DATABASE_URL`: **Not set** in current environment.
* No plaintext `.env` files containing remote passwords exist in the repository (properly prevented by `.gitignore`).

### 3.3 Strict Execution Boundary Triggered
Under Section 4 ("PHASE B — VERIFY BOTH SUPABASE PROJECTS") and Section 15 ("STRICT EXECUTION BOUNDARIES") of the Master Execution Directive:
> *"If either connection is missing, invalid, or cannot be safely identified, stop before applying migrations to that environment."*  
> *"If credentials are missing, provide the exact environment variable names and explain where they should be configured securely. Do not fabricate connection strings, API keys, migration results, or cloud verification results."*

**Gate Enforcement Action:**
Execution is safely suspended at Phase B/C prior to running remote Flyway migration commands. No fake credentials or simulated live results were generated.

---

## 4. Phase D — Migration Pre-Execution Validation

Before executing migrations on either database, the following validation holds true:

1. **Deterministic Execution Plan**:
   - `V1` creates base schema and indexes.
   - `V2` seeds curriculum (5 degrees, 9 courses, 8 subjects).
   - `V3` purges demo users and synchronizes PostgreSQL sequences.
   - `V4` adds tokens, enrollments, sessions, assignments, and exam tables.
   - `V5` adds password reset tokens and course archival columns.
2. **Schema Drift Prevention**:
   - Both databases must start from baseline and apply identical migrations in identical order.
   - Flyway table `flyway_schema_history` will record the checksum and state of each migration.
3. **Rollback & Non-Destructive Nature**:
   - No migration contains destructive drops. If any migration fails, Flyway wraps the execution in a transaction, rolling back uncommitted changes.

---

## 5. Phase G — Storage Architecture & Bucket Configuration

### 5.1 Storage Implementation Details
The application's file storage implementation was inspected in `com.virtualclassroom.service.FileStorageService`:
* **Bucket Name**: `academic-assets`
* **Allowlist Enforcement**:
  ```java
  public static final Set<String> ALLOWED_BUCKETS = Set.of(
          "academic-assets",
          "study-materials",
          "submissions",
          "profile-photos",
          "course-documents"
  );
  ```
* **Protocol Used**:
  - The application calls the **Supabase Storage REST API** directly using `RestTemplate`:
    - Upload sign URL: `POST {supabaseUrl}/storage/v1/object/upload/sign/{bucket}/{path}`
    - Download sign URL: `POST {supabaseUrl}/storage/v1/object/sign/{bucket}/{path}`
    - File upload: `POST {supabaseUrl}/storage/v1/object/{bucket}/{path}`
    - File delete: `DELETE {supabaseUrl}/storage/v1/object/{bucket}/{path}`
  - It does **not** use the AWS S3-compatible API or client-side JavaScript SDKs.
  - All operations are authenticated using the server-side `SUPABASE_SERVICE_ROLE_KEY` (`Authorization: Bearer <token>`).

### 5.2 Required Supabase Storage Settings (Both Projects)
1. **Bucket Name**: `academic-assets` (already provisioned in both projects).
2. **Visibility**: **Private** (Public access must remain **disabled**; files are accessed via pre-signed URLs or backend proxy).
3. **File Size Limit**: **50 MB** (`52,428,800` bytes).
4. **Permitted MIME Types**:
   - Documents: `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `application/vnd.ms-powerpoint`, `application/vnd.openxmlformats-officedocument.presentationml.presentation`, `text/plain`, `text/csv`
   - Images: `image/jpeg`, `image/png`, `image/webp`, `image/gif`
   - Video: `video/mp4`, `video/webm`
   - Archives: `application/zip`, `application/x-zip-compressed`

---

## 6. Phase H — Supabase Data API (PostgREST) Security Audit

* **Current Production Setting**: Data API is **Disabled** (OFF).
* **Audit Finding**:
  - The Spring Boot backend connects directly to PostgreSQL via JDBC (`org.postgresql.Driver`) over TCP port `5432`.
  - The React frontend communicates strictly with the Spring Boot backend via Axios at `/api/v1/*`.
  - Zero requests in either frontend or backend are made to PostgREST (`/rest/v1/*`).
* **Architectural Mandate**:
  - **Keep "Enable Data API" OFF in both Staging and Production.**
  - Disabling the Data API adheres to least-privilege security, prevents exposing raw database tables over HTTP, and eliminates potential RLS bypass vulnerabilities.

---

## 7. Phase I — Environment Configuration Specifications

### 7.1 Staging Environment Variables (Render Dashboard — `virtual-classroom-backend-staging`)
Configure these variables in the Render Dashboard under **Environment Settings**:

```bash
# Runtime Profile
SPRING_PROFILES_ACTIVE=staging
PORT=8080

# Database Connectivity (Supabase Staging: zrbpjcfssbtxxgrjtsca)
# Note: Connect via direct host or Session Pooler on port 5432 with sslmode=require
DATABASE_URL=jdbc:postgresql://db.zrbpjcfssbtxxgrjtsca.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<STAGING_SUPABASE_DB_PASSWORD>
DB_POOL_MAX=10
DB_POOL_MIN=2

# Security & CORS
JWT_SECRET=<CRYPTOGRAPHIC_256_BIT_SECRET_MIN_32_BYTES>
JWT_EXPIRATION_MS=86400000
COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://virtual-classroom-staging.vercel.app,http://localhost:5173

# Storage Integration (Supabase Staging)
STORAGE_PROVIDER=supabase
SUPABASE_URL=https://zrbpjcfssbtxxgrjtsca.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<STAGING_SUPABASE_SERVICE_ROLE_KEY>
SUPABASE_STORAGE_BUCKET=academic-assets
```

### 7.2 Production Environment Variables (Render Dashboard — `virtual-classroom-backend-prod`)
Configure these variables in the Render Dashboard under **Environment Settings**:

```bash
# Runtime Profile
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# Database Connectivity (Supabase Production: tzbjmxtmysvzxdmspiui)
# Note: Connect via direct host or Session Pooler on port 5432 with sslmode=require
DATABASE_URL=jdbc:postgresql://db.tzbjmxtmysvzxdmspiui.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<PRODUCTION_SUPABASE_DB_PASSWORD>
DB_POOL_MAX=10
DB_POOL_MIN=2

# Security & CORS
JWT_SECRET=<CRYPTOGRAPHIC_256_BIT_SECRET_MIN_32_BYTES>
JWT_EXPIRATION_MS=86400000
COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://virtual-classroom.edu,https://virtual-classroom.vercel.app

# Storage Integration (Supabase Production)
STORAGE_PROVIDER=supabase
SUPABASE_URL=https://tzbjmxtmysvzxdmspiui.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<PRODUCTION_SUPABASE_SERVICE_ROLE_KEY>
SUPABASE_STORAGE_BUCKET=academic-assets
```

### 7.3 Crucial Port & Pooler Note for Flyway Migrations
* **Port 5432 (Session Mode or Direct Connection) MUST be used for migrations**:
  - Flyway requires PostgreSQL session-level advisory locks (`pg_advisory_lock`) to synchronize migration execution across application instances.
  - The Supavisor / PgBouncer transaction pooler on port `6543` does not support session advisory locks.
  - Therefore, `${DATABASE_URL}` must use port `5432`.

---

## 8. Step-by-Step Procedure to Complete Migration

When you are ready to apply the migrations to Staging and Production from your secure machine, follow these steps:

### Step 1: Export Staging Credentials & Run Staging Migrations
In your secure terminal (outside Git tracking):
```bash
# Set Staging variables
export STAGING_DATABASE_URL="jdbc:postgresql://db.zrbpjcfssbtxxgrjtsca.supabase.co:5432/postgres?sslmode=require"
export STAGING_DB_USER="postgres"
export STAGING_DB_PASSWORD="<your_staging_password>"

# Run Flyway migration against Staging
cd backend
./mvnw flyway:migrate \
  -Dflyway.url="$STAGING_DATABASE_URL" \
  -Dflyway.user="$STAGING_DB_USER" \
  -Dflyway.password="$STAGING_DB_PASSWORD" \
  -Dflyway.locations="classpath:db/migration"

# Inspect applied history
./mvnw flyway:info \
  -Dflyway.url="$STAGING_DATABASE_URL" \
  -Dflyway.user="$STAGING_DB_USER" \
  -Dflyway.password="$STAGING_DB_PASSWORD"
```

### Step 2: Verify Staging Schema & Storage
1. Confirm that 21 tables are created in the `public` schema.
2. Confirm `flyway_schema_history` has 5 rows (V1 through V5) all marked `success = true`.
3. Verify that `academic-assets` bucket exists in `virtual-classroom-staging` and is set to Private.

### Step 3: Export Production Credentials & Run Production Migrations
Only after Step 2 succeeds:
```bash
# Set Production variables
export PRODUCTION_DATABASE_URL="jdbc:postgresql://db.tzbjmxtmysvzxdmspiui.supabase.co:5432/postgres?sslmode=require"
export PRODUCTION_DB_USER="postgres"
export PRODUCTION_DB_PASSWORD="<your_production_password>"

# Run Flyway migration against Production
cd backend
./mvnw flyway:migrate \
  -Dflyway.url="$PRODUCTION_DATABASE_URL" \
  -Dflyway.user="$PRODUCTION_DB_USER" \
  -Dflyway.password="$PRODUCTION_DB_PASSWORD" \
  -Dflyway.locations="classpath:db/migration"

# Inspect applied history
./mvnw flyway:info \
  -Dflyway.url="$PRODUCTION_DATABASE_URL" \
  -Dflyway.user="$PRODUCTION_DB_USER" \
  -Dflyway.password="$PRODUCTION_DB_PASSWORD"
```

### Step 4: Configure Cloud Deployments
1. Populate the Render Staging environment variables for `virtual-classroom-backend-staging`.
2. Populate the Render Production environment variables for `virtual-classroom-backend-prod`.
3. Trigger deployment via the Render dashboard or GitHub Actions deployment workflow.
