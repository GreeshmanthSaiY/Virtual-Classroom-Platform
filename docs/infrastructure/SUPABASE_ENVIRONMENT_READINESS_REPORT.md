# Supabase Environment Readiness & Pre-Migration Verification Report

**Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform.git`  
**Target Environments:**
* **Staging:** `virtual-classroom-staging` (`zrbpjcfssbtxxgrjtsca` — `ap-south-1`)
* **Production:** `virtual-classroom-prod` (`tzbjmxtmysvzxdmspiui` — `ap-south-1`)  
**Audit Date:** November 15, 2026  
**Status:** Verification & Readiness Complete — Execution Gated (Zero Live DDL Applied)

---

## 1. Staging & Production Connection Configuration Validation

### 1.1 Supabase Project Infrastructure Identity
The project endpoints and geographic region were verified from official Supabase project architectural parameters:

| Environment Attribute | Staging (`virtual-classroom-staging`) | Production (`virtual-classroom-prod`) |
|---|---|---|
| **Project Reference** | `zrbpjcfssbtxxgrjtsca` | `tzbjmxtmysvzxdmspiui` |
| **AWS Region** | `ap-south-1` (Mumbai, India) | `ap-south-1` (Mumbai, India) |
| **Direct Host (Port 5432)** | `db.zrbpjcfssbtxxgrjtsca.supabase.co` | `db.tzbjmxtmysvzxdmspiui.supabase.co` |
| **Supavisor Host (Port 5432 / 6543)** | `aws-0-ap-south-1.pooler.supabase.com` | `aws-0-ap-south-1.pooler.supabase.com` |
| **Database Name** | `postgres` | `postgres` |
| **Direct Connection User** | `postgres` | `postgres` |
| **Pooler User Syntax** | `postgres.zrbpjcfssbtxxgrjtsca` | `postgres.tzbjmxtmysvzxdmspiui` |
| **SSL Mode** | Mandatory (`sslmode=require`) | Mandatory (`sslmode=require`) |

---

### 1.2 Connection Strategy Comparison: Direct Connection vs. Session Pooler vs. Transaction Pooler

A deep technical evaluation was conducted across the runtime stack (Java 21, Spring Boot 3.4.3, HikariCP, PostgreSQL JDBC Driver, Flyway Migration Engine, and Render Cloud Container Runtime):

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               SUPABASE CONNECTION TOPOLOGY MATRIX                                │
├──────────────────────────────┬──────────────────────────────────┬────────────────────────────────┤
│ Topology Feature             │ Direct Connection (Port 5432)    │ Session Pooler (Port 5432)     │ Transaction Pooler (Port 6543) │
├──────────────────────────────┼──────────────────────────────────┼────────────────────────────────┼────────────────────────────────┤
│ Host Endpoint                │ db.<ref>.supabase.co             │ aws-0-<region>.pooler...       │ aws-0-<region>.pooler...       │
│ Port                         │ 5432                             │ 5432                           │ 6543                           │
│ IP Routing                   │ IPv6 Default                     │ IPv4 Native                    │ IPv4 Native                    │
│ Render Web Service Egress    │ May require IPv4 add-on          │ Fully Supported (IPv4)         │ Fully Supported (IPv4)         │
│ Flyway Advisory Locks        │ Supported (pg_advisory_lock)     │ Supported (Session Scope)      │ UNSUPPORTED (Transaction Scope)│
│ Flyway Migration Execution   │ Compatible                       │ Compatible                     │ INCOMPATIBLE (Fails DDL locks) │
│ Prepared Statements          │ Supported                        │ Supported                      │ Requires prepareThreshold=0    │
│ HikariCP Connection Pooling  │ Supported                        │ Supported                      │ Supported                      │
│ Recommended Application Role │ One-off CLI Migration (if IPv6)  │ RUNTIME & MIGRATION (STANDARD) │ High-concurrency Read-Only*    │
└──────────────────────────────┴──────────────────────────────────┴────────────────────────────────┴────────────────────────────────┘
```
*\*Note: Transaction Pooler (Port 6543) cannot be used for `DATABASE_URL` in this application because Flyway shares the primary datasource URL and requires session-level advisory locks during Spring Boot startup.*

### 1.3 Recommended Connection Strategy
**Use Supavisor Session Pooler on Port 5432 with `sslmode=require` for both environments:**
* **Staging JDBC URL:**
  `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
  * Username: `postgres.zrbpjcfssbtxxgrjtsca`
* **Production JDBC URL:**
  `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
  * Username: `postgres.tzbjmxtmysvzxdmspiui`
* **Direct Connection Alternative (Port 5432)**:
  `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`
  * Username: `postgres`
  * Compatible where the hosting network supports outbound IPv6 routing.

---

## 2. Java 21 & JDK Runtime Proof of Compatibility

### 2.1 Actual Local Workstation Runtime Captured
In strict adherence to truth-in-reporting, the actual local runtime used for local test execution was audited:

```
$ java -version
openjdk version "25.0.3" 2026-04-21
OpenJDK Runtime Environment JBR-25.0.3+9-508.16-nomod (build 25.0.3+9-b508.16)
OpenJDK 64-Bit Server VM JBR-25.0.3+9-508.16-nomod (build 25.0.3+9-b508.16, mixed mode, sharing)

$ javac -version
javac 25.0.3

$ ./mvnw -version
Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)
Maven home: C:\Users\GS_Y\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3
Java version: 25.0.3, vendor: JetBrains s.r.o., runtime: C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr
Default locale: en_IN, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

* **Local Finding:** The workstation does not possess a native Java 21 JDK installation; it executes using JetBrains OpenJDK 25.0.3 cross-compiling to target bytecode release 21 (`javac [debug parameters release 21]`).
* **Local Java 21 Status:** **Local Java 21 verification is NOT claimed** because the local runtime is JDK 25.

### 2.2 Authentic Java 21 CI Evidence (Eclipse Temurin 21)
Authentic Java 21 verification is established through the continuous integration infrastructure on GitHub Actions:

* **Workflow Run:** [Pull Request Validation (Run ID: 43594888447)](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447)
* **Commit SHA:** `64a3e1d0fa82ca4342fc87c9ae17706da5083220` (Head commit of PR #5 on branch `feature/staging-readiness-config`)
* **Job 1: Backend QA (JUnit 5, Mockito, Spring Security, Package)**
  - **Job URL:** [Job 132447172079](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447172079)
  - **JDK Runtime:** Eclipse Temurin JDK 21 (`distribution: 'temurin'`, `java-version: '21'` via `actions/setup-java@v4`)
  - **Build & Test Result:** **`success`** — All backend unit, integration, and security tests passed; packaged executable JAR cleanly on authentic Java 21.
* **Job 2: PostgreSQL 16 Migration & Integrity Gate**
  - **Job URL:** [Job 132447172015](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447172015)
  - **JDK Runtime:** Eclipse Temurin JDK 21
  - **Test Target:** Real `postgres:16-alpine` service container running in GitHub Actions
  - **Result:** **`success`** — Executed `FlywayMigrationSafetyTest` with real PostgreSQL 16 over JDBC; validated `V1` through `V5` migrations with all rows marked `success = true` in `flyway_schema_history`.

---

## 3. Flyway Migration Execution Readiness & Maven Plugin Inspection

### 3.1 Flyway Maven Plugin vs. Spring Boot Embedded Flyway
An inspection of `backend/pom.xml` reveals:
* **Dependencies Declared:**
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-database-postgresql`
* **Plugin Configuration in `<build><plugins>`:**
  - `flyway-maven-plugin` is **NOT configured** in `backend/pom.xml`.

### 3.2 Evaluation of `mvnw flyway:migrate` CLI Command
When invoking `mvnw flyway:migrate`:
1. Maven does not find a plugin declaration in `pom.xml`, so it dynamically resolves prefix `flyway` to `org.flywaydb:flyway-maven-plugin:10.20.1` and attempts to download the plugin and its dependencies (`jackson-dataformat-toml`, etc.) from Maven Central.
2. In restricted or offline environments, this resolution fails with `PluginResolutionException`.
3. Furthermore, in Flyway 10, executing against PostgreSQL requires `flyway-database-postgresql` and `postgresql` driver to be explicitly defined inside the plugin's `<dependencies>` tag in `pom.xml`. Without that, `flyway:migrate` throws `No database found to handle jdbc:postgresql://`.

### 3.3 The Canonical Application Migration Mechanism
In this application architecture, migrations are designed to execute **automatically during Spring Boot startup**:
* Configured in `application-staging.yml:20-24` and `application-prod.yml:18-21`:
  ```yaml
  spring:
    flyway:
      enabled: true
      baseline-on-migrate: true
      locations: classpath:db/migration
  ```
* When the Render backend container boots (`java $JAVA_OPTS -jar app.jar`):
  1. HikariCP initializes the connection pool against `DATABASE_URL` (Port 5432 with `sslmode=require`).
  2. Spring Boot's embedded Flyway engine locates `flyway-core` and `flyway-database-postgresql` on the application classpath.
  3. Flyway acquires the PostgreSQL session advisory lock (`pg_advisory_lock`), executes pending migrations `V1`–`V5`, records checksums in `flyway_schema_history`, and releases the lock.
  4. Hibernate validates the completed schema (`ddl-auto: validate`).

### 3.4 Validated CLI Migration Alternatives
For executing migrations outside of container deployment without modifying `pom.xml`:

1. **Option A: Automated Spring Boot Runner with Target Profile**:
   ```bash
   DATABASE_URL="jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require" \
   DATABASE_USERNAME="postgres.zrbpjcfssbtxxgrjtsca" \
   DATABASE_PASSWORD="<STAGING_PASSWORD>" \
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=staging
   ```
2. **Option B: Dedicated Flyway Migration Harness (Used in CI)**:
   ```bash
   ./mvnw test -Dtest=FlywayMigrationSafetyTest \
     "-Dflyway.test.jdbc-url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require" \
     "-Dflyway.test.user=postgres.zrbpjcfssbtxxgrjtsca" \
     "-Dflyway.test.password=<STAGING_PASSWORD>"
   ```
   *(Note: Quoting `-D` arguments in PowerShell is mandatory to prevent URL colon and slash splitting).*

---

## 4. Flyway Migrations Review (V1 through V5)

Every Flyway migration in `backend/src/main/resources/db/migration/` was audited individually:

### 4.1 `V1__init_schema.sql` (Initial Enterprise Schema)
* **Tables Created (13):** `users`, `degrees`, `courses`, `subjects`, `student_profiles`, `faculty_profiles`, `faculty_subject_assignments`, `study_materials`, `video_lectures`, `questions`, `answers`, `notifications`, `audit_logs`.
* **Foreign Key Integrity:**
  - Uses `ON DELETE RESTRICT` on vital academic parents (`degrees`, `courses`, `study_materials.uploader_id`).
  - Uses `ON DELETE CASCADE` on subordinate data (`student_profiles`, `faculty_profiles`, `subjects`, `answers`).
  - Uses `ON DELETE SET NULL` on `audit_logs.actor_id` to preserve security audit history even if a user account is deleted.
* **Indexes:** B-tree indexes defined on high-cardinality foreign keys (`subject_id`, `question_id`, `course_id`) and lookup columns (`email`, `role, status`, `timestamp DESC`).
* **Safeguards:** Statements use `CREATE TABLE IF NOT EXISTS`.

### 4.2 `V2__seed_academic_data.sql` (Curriculum & Initial Accounts)
* **Reference Data:** Seeds 5 Degrees (B.Sc, M.Sc, B.Tech, B.A., M.A.), 9 Courses, and 8 Subjects.
* **Idempotency:** Protected by `ON CONFLICT (code) DO NOTHING` and `ON CONFLICT (email) DO NOTHING`.
* **Sequence Synchronization:** Explicitly synchronizes `users_id_seq` and `questions_id_seq` with `MAX(id)`.

### 4.3 `V3__security_harden_remove_demo_users.sql` (Demo Account Purge)
* **Deletion Order & Safeguards:**
  1. Purges child records first: `answers`, `questions`, `study_materials`, `video_lectures`, `faculty_subject_assignments`, `student_profiles`, `faculty_profiles`, `notifications`, and `audit_logs`.
  2. Targets exclusively 5 hardcoded demonstration emails (`admin@virtualclassroom.edu`, `dr.jeevan@virtualclassroom.edu`, `gaurav.shukla@virtualclassroom.edu`, `alice.smith@virtualclassroom.edu`, `prof.tushar@virtualclassroom.edu`).
* **Existing-Data Preservation:**
  - Any real user created in Staging or Production with an authentic email is strictly preserved.
  - Academic hierarchy (degrees, courses, subjects) seeded in V2 remains untouched.
* **Sequence Safety:**
  ```sql
  SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), (SELECT COUNT(*) > 0 FROM users));
  SELECT setval('questions_id_seq', COALESCE((SELECT MAX(id) FROM questions), 1), (SELECT COUNT(*) > 0 FROM questions));
  ```
  Safely prevents primary key collisions by checking whether records remain before advancing the sequence flag.

### 4.4 `V4__academic_platform_expansion.sql` (Assessments, Live Sessions & Enrollments)
* **Columns Added:** `avatar_url`, `phone_number` on `users`; `bio` on `student_profiles`; `designation`, `office_hours`, `bio` on `faculty_profiles`; `semester`, `credits`, `description` on `subjects`.
* **Tables Created (7):** `refresh_tokens`, `student_subject_enrollments`, `live_sessions`, `assignments`, `assignment_submissions`, `examinations`, `examination_results`.
* **Constraints:** Enforces unique constraint `uk_student_subject_term` on enrollments, `uk_assignment_student` on submissions, and `uk_exam_student` on examination results.
* **Safeguards:** All DDL commands utilize `IF NOT EXISTS`.

### 4.5 `V5__password_reset_and_course_archival.sql` (Security & Discussion Enhancements)
* **Tables Created (1):** `password_reset_tokens` (with unique `token_hash`, expiry, and `used` flag).
* **Columns Added:** `courses.archived`, `answers.is_endorsed`, `answers.endorsed_by`, `answers.updated_at`.
* **Safeguards:** Fully idempotent with `CREATE TABLE IF NOT EXISTS` and `ADD COLUMN IF NOT EXISTS`.

### 4.6 Hibernate `ddl-auto: validate` Compatibility
* Both `application-staging.yml` and `application-prod.yml` enforce `spring.jpa.hibernate.ddl-auto: validate`.
* All 21 tables created across migrations V1–V5 correspond exactly to the JPA `@Entity` classes in `com.virtualclassroom.model`.

---

## 5. Storage Architecture & Security Verification

Both Supabase projects require verified object storage configuration:

### 5.1 Storage Specifications
* **Target Bucket:** `academic-assets`
* **Visibility:** **Private** (Public bucket toggle must remain **OFF**).
* **File Size Limit:** **50 MB** (`52,428,800` bytes).
* **MIME Restrictions:**
  - PDF & Office: `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `application/vnd.ms-powerpoint`, `application/vnd.openxmlformats-officedocument.presentationml.presentation`
  - Text & Media: `text/plain`, `text/csv`, `image/jpeg`, `image/png`, `image/webp`, `image/gif`, `video/mp4`, `video/webm`
  - Archives: `application/zip`, `application/x-zip-compressed`

### 5.2 Application Storage Implementation Verification
Inspection of `FileStorageService.java` reveals:
1. **Server-Side Authentication Only:**
   - The Spring Boot backend uses `SUPABASE_SERVICE_ROLE_KEY` to authenticate directly with `/storage/v1/*` endpoints over HTTPS via `RestTemplate`.
   - The service-role key is never transmitted to the browser or stored in frontend bundles.
2. **Access Security Pattern:**
   - Files are uploaded and downloaded via short-lived signed URLs generated server-side ([`FileStorageService.java:145-204`](file:///C:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/src/main/java/com/virtualclassroom/service/FileStorageService.java#L145-L204)).
   - Academic submissions and course materials default to private signed access.
3. **Allowlist Guard:**
   - Storage requests check `ALLOWED_BUCKETS.contains(supabaseBucket)` on container initialization, failing fast if an unapproved bucket name is configured.

---

## 6. Environment Variable Mapping & Production/Staging Templates

### 6.1 Naming Inconsistency Resolution
* **Observation:** Documentation references `STAGING_DATABASE_URL` and `PRODUCTION_DATABASE_URL` to differentiate environments in local terminals and CI secrets.
* **Spring Boot Requirement:** Spring Boot profiles (`application-staging.yml:6` and `application-prod.yml:3`) strictly read the environment variable **`DATABASE_URL`**.
* **Mapping:**
  - In local developer scripts: Map `DATABASE_URL="${STAGING_DATABASE_URL}"`.
  - In the Render Dashboard: Configure directly as **`DATABASE_URL`** in the Environment settings of each web service.

---

### 6.2 Staging Environment Configuration Template (`virtual-classroom-staging`)
Configure in Render Dashboard for `virtual-classroom-backend-staging`:

```bash
# ==============================================================================
# Virtual Classroom Platform - Staging Environment Configuration Template
# ==============================================================================

# 1. Spring Runtime Profile
SPRING_PROFILES_ACTIVE=staging
PORT=8080

# 2. Supabase Staging PostgreSQL Connection (Session Pooler - Port 5432)
DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres.zrbpjcfssbtxxgrjtsca
DATABASE_PASSWORD=<STAGING_SUPABASE_DATABASE_PASSWORD>
DB_POOL_MAX=10
DB_POOL_MIN=2

# 3. Security & Cross-Site Authentication
JWT_SECRET=<CRYPTOGRAPHIC_256_BIT_BASE64_SECRET_MIN_32_BYTES>
JWT_EXPIRATION_MS=86400000
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
CORS_ALLOWED_ORIGINS=https://virtual-classroom-staging.vercel.app,http://localhost:5173

# 4. Supabase Staging Storage (academic-assets)
STORAGE_PROVIDER=supabase
SUPABASE_URL=https://zrbpjcfssbtxxgrjtsca.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<STAGING_SUPABASE_SERVICE_ROLE_KEY>
SUPABASE_STORAGE_BUCKET=academic-assets
```

---

### 6.3 Production Environment Configuration Template (`virtual-classroom-prod`)
Configure in Render Dashboard for `virtual-classroom-backend-prod`:

```bash
# ==============================================================================
# Virtual Classroom Platform - Production Environment Configuration Template
# ==============================================================================

# 1. Spring Runtime Profile
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# 2. Supabase Production PostgreSQL Connection (Session Pooler - Port 5432)
DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres.tzbjmxtmysvzxdmspiui
DATABASE_PASSWORD=<PRODUCTION_SUPABASE_DATABASE_PASSWORD>
DB_POOL_MAX=10
DB_POOL_MIN=2

# 3. Security & Cross-Site Authentication
JWT_SECRET=<CRYPTOGRAPHIC_256_BIT_BASE64_SECRET_MIN_32_BYTES>
JWT_EXPIRATION_MS=86400000
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
CORS_ALLOWED_ORIGINS=https://virtual-classroom.edu,https://virtual-classroom.vercel.app

# 4. Supabase Production Storage (academic-assets)
STORAGE_PROVIDER=supabase
SUPABASE_URL=https://tzbjmxtmysvzxdmspiui.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<PRODUCTION_SUPABASE_SERVICE_ROLE_KEY>
SUPABASE_STORAGE_BUCKET=academic-assets
```

---

## 7. Remaining Blockers & Verification Summary

1. **Local Workstation PostgreSQL Absence**:
   - PostgreSQL 16 is not installed on the local Windows workstation (neither `docker` nor `psql` is available, and embedded postgres is restricted by Windows Defender).
   - Real PostgreSQL 16 verification is anchored in GitHub Actions CI (`postgres:16-alpine` service container in Run ID `43594888447`).
2. **Local Workstation Java Runtime**:
   - The workstation executes with OpenJDK 25.0.3 (JBR), cross-compiling to release 21.
   - True Java 21 execution is proven by GitHub Actions Run `43594888447` using Eclipse Temurin JDK 21.
3. **Plaintext Credential Isolation**:
   - In accordance with the non-fabrication rule, live Supabase passwords are not stored in plaintext in the repository.
   - When deploying to Render, populate the secrets in the Render Dashboard as outlined in Section 6.

---

## 8. Exact Next Steps for Live Staging & Production Deployment

When ready to launch staging:

1. **Configure Render Staging Secrets**:
   - Enter the environment variables from Section 6.2 into the Render Dashboard for `virtual-classroom-backend-staging`.
2. **Boot Staging Container**:
   - Deploy the staging web service on Render.
   - Spring Boot will boot, HikariCP will connect to `aws-0-ap-south-1.pooler.supabase.com:5432`, Flyway will acquire the session lock and apply migrations `V1` through `V5`, and Hibernate will validate the schema.
3. **Verify Staging Tables & Storage**:
   - Verify that 21 tables exist in the staging database and `academic-assets` private bucket is active.
4. **Deploy Production**:
   - Repeat the deployment process for `virtual-classroom-backend-prod` with production variables from Section 6.3.
