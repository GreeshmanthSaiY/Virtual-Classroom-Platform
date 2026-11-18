# MILESTONE 18.3 — STAGING SECURITY REMEDIATION IMPLEMENTATION REPORT
## Virtual Classroom Platform — Evidence-Backed Staging Hardening Record

**Execution Date:** 2026-11-18  
**Branch:** `feature/staging-readiness-config`  
**Implementation Commit:** `7cc23ac` (`7cc23ac837adba48d89a2dd172b11ec10833d780`)  
**Target Environment:** Supabase **STAGING ONLY** (`virtual-classroom-staging`, Ref: `zrbpjcfssbtxxgrjtsca`, AWS `ap-south-1` Mumbai)  
**Database Engine:** `PostgreSQL 17.11 on aarch64-unknown-linux-gnu, compiled by gcc (GCC) 15.2.0, 64-bit`  
**Session Pooler:** `aws-0-ap-south-1.pooler.supabase.com:5432`  
**Production Boundary:** Production project `tzbjmxtmysvzxdmspiui` was **100% untouched** (zero connections, zero queries, zero config pollution).  
**Deployment Boundary:** Render and Vercel deployments were **NOT** triggered; branch was **NOT** merged into `develop` or `main`.

---

## 1. Executive Summary

All authorized **Milestone 18.3** staging security remediations designed and validated in **Milestone 18.2.1** have been implemented, executed against the live Supabase staging database, and verified through automated unit/integration tests, live Supavisor pooler gates, Hibernate schema validation, and GitHub Actions CI.

### High-Level Outcome Summary
- **Runtime Database Role Provisioned & Verified:** Dedicated unprivileged role `vc_app_user` created on staging with `LOGIN`, `NOSUPERUSER`, `NOCREATEDB`, `NOCREATEROLE`, `NOBYPASSRLS`, `NOINHERIT`, `CONNECTION LIMIT 20`, and **0 owned database objects**.
- **Supavisor Pooler Gate Passed:** `vc_app_user.zrbpjcfssbtxxgrjtsca` authenticated cleanly over **TLSv1.3** (`TLS_AES_256_GCM_SHA384`), passed **13/13 negative privilege tests** (DDL, role/DB creation, `flyway_schema_history`, `audit_logs` UPDATE/DELETE, hard-deletes, read-only catalog writes), passed **6/6 rolled-back positive DML tests**, and proved **`NOBYPASSRLS`** live via a zero-policy RLS probe table.
- **Object-by-Object ACL Remediation Completed:** Revoked all `anon`, `authenticated`, and `PUBLIC` privileges strictly on the **22 `postgres`-owned tables** and **18 `postgres`-owned sequences** in `public`, plus scoped `ALTER DEFAULT PRIVILEGES FOR ROLE postgres`. Preserved `public.rls_auto_enable()`, all Supabase-managed schemas (`storage`, `auth`, `realtime`, etc.), `storage.buckets` (`academic-assets`), and RLS enabled across all 22 tables.
- **Double-Run Idempotency Proven:** Re-executing both `M18_3_1_provision_vc_app_user.sql` and `M18_3_2_revoke_app_object_acls.sql` converged cleanly with zero inconsistent grants or errors.
- **Hibernate & HikariCP Verified Under `vc_app_user`:** 10 concurrent HikariCP pooled queries and full Hibernate `ddl-auto: validate` across all **21 JPA entities** passed 100% connecting strictly as `vc_app_user.zrbpjcfssbtxxgrjtsca`.
- **Application Security & Storage Hardened:** Implemented official Supabase Storage REST API signed URLs (`Authorization: Bearer` + `apikey`), domain-table download ownership authorization (`StorageAuthorizationService`), 14-MIME bucket parity, fail-closed `staging`/`prod` storage error handling, staging JWT fallback-secret rejection, PostgreSQL JDBC HikariCP settings, disabled runtime Flyway in `staging`/`prod`, removed `localhost` from staging CORS, and suppressed internal exception disclosure.

---

## 2. Exact Source Changes

### 2.1 Application Java Source Files

| File | Action | Exact Security Changes |
| :--- | :---: | :--- |
| `backend/src/main/java/com/virtualclassroom/exception/StorageOperationException.java` | **Created** | Dedicated runtime exception for upstream cloud storage failures in `staging`/`prod` where silent local fallback is prohibited. |
| `backend/src/main/java/com/virtualclassroom/exception/GlobalExceptionHandler.java` | **Modified** | Added sanitized handlers for `StorageOperationException` (HTTP `502 Bad Gateway`, generic client message, zero secret/URL disclosure) and `SecurityException` (HTTP `400 Bad Request`, generic path-rejection message). |
| `backend/src/main/java/com/virtualclassroom/service/StorageAuthorizationService.java` | **Created** | Enforces fail-closed domain-table download authorization (`authorizeDownload`) before issuing signed URLs: checks `assignment_submissions` (owner student or assigned faculty), `study_materials` & `assignments` (enrolled student or assigned faculty/uploader), `profile-photos/*` (authenticated classroom members), unlinked uploader namespace, and `ROLE_ADMIN`; rejects all unmatched paths with `AccessDeniedException` (HTTP `403`). |
| `backend/src/main/java/com/virtualclassroom/service/FileStorageService.java` | **Modified** | 1. Uses official Supabase Storage REST API (`POST /storage/v1/object/sign/{bucket}/{path}`, `POST /storage/v1/object/upload/sign/{bucket}/{path}`, `POST/DELETE /storage/v1/object/{bucket}/{path}`) with **both** `Authorization: Bearer` and `apikey` headers.<br>2. Adds `x-upsert: false` on uploads and signed-upload creation.<br>3. Eliminates `/storage/v1/object/public/` URL generation (`uploadToSupabase` returns a 900s signed download URL; `generateSignedUploadUrl` returns `/api/v1/storage/signed-download-url?path=...`).<br>4. Sets default signed download expiry to `900s` (clamped `60..3600s`).<br>5. Adds strict `validateStoragePath()` canonicalization regex blocking `..`, `%2e`, `%2f`, `\\`, leading/trailing `/`, `//`, null bytes, and query strings.<br>6. Reconciles `ALLOWED_MIME_TYPES` 1:1 with the **14** exact MIME types configured on live staging bucket `academic-assets`.<br>7. Fails closed (`StorageOperationException` / startup `IllegalStateException`) in **both `staging` and `prod`** profiles with zero local disk fallback. |
| `backend/src/main/java/com/virtualclassroom/controller/StorageController.java` | **Modified** | Enforces `validateStoragePath` + `StorageAuthorizationService.authorizeDownload` in `GET /api/v1/storage/signed-download-url` (default `expiresIn=900`); blocks `GET /api/v1/storage/files/**` with HTTP `403 Forbidden` whenever cloud storage is active. |
| `backend/src/main/java/com/virtualclassroom/repository/StudyMaterialRepository.java` | **Modified** | Added `findByStoragePath(@Param("path") String path)` query for domain-table ownership lookup. |
| `backend/src/main/java/com/virtualclassroom/repository/AssignmentRepository.java` | **Modified** | Added `findByAttachmentStoragePath(@Param("path") String path)` query for domain-table ownership lookup. |
| `backend/src/main/java/com/virtualclassroom/repository/AssignmentSubmissionRepository.java` | **Modified** | Added `findBySubmissionStoragePath(@Param("path") String path)` query for domain-table ownership lookup. |
| `backend/src/main/java/com/virtualclassroom/security/SecurityConfig.java` | **Modified** | Restricted `/api/v1/storage/files/**` `permitAll()` strictly to `app.storage.provider=local`; added `/actuator/health/**` so Render's `/actuator/health/liveness` probe is permitted. |
| `backend/src/main/java/com/virtualclassroom/security/JwtTokenProvider.java` | **Modified** | Extended `LOCAL_DEV_FALLBACK_SECRET` rejection to fail fast on `staging` profile in addition to `prod`/`production`. |

### 2.2 Configuration, Deployment Manifests & SQL Scripts

| File | Action | Exact Changes |
| :--- | :---: | :--- |
| `render.yaml` | **Modified** | Staging: set `DATABASE_USERNAME` to `vc_app_user.zrbpjcfssbtxxgrjtsca`; removed `http://localhost:5173` from `CORS_ALLOWED_ORIGINS` (`https://virtual-classroom-staging.vercel.app` only).<br>Production: changed `DATABASE_USERNAME` from unqualified `postgres` to `sync: false` (zero staging values). |
| `backend/src/main/resources/application-staging.yml` | **Modified** | Disabled runtime Flyway (`enabled: false`, `baseline-on-migrate: false`); added `server.error.include-message: never` & `include-binding-errors: never`; added HikariCP `leak-detection-threshold: 30000`, `validation-timeout: 5000`, `initialization-fail-timeout: 1`, PostgreSQL JDBC properties (`sslmode: require`, `prepareThreshold: 5`, `preparedStatementCacheQueries: 256`, `preparedStatementCacheSizeMiB: 5`, `ApplicationName: virtual-classroom-staging`); removed `localhost` from default staging CORS. |
| `backend/src/main/resources/application-prod.yml` | **Modified** | Applied matching hardened Flyway-disabled, error-suppression, HikariCP, and PostgreSQL JDBC settings (`DB_POOL_MAX:15`, `DB_POOL_MIN:5`, `ApplicationName: virtual-classroom-prod`); verified zero staging references. |
| `backend/src/main/resources/application.yml` | **Modified** | Changed `spring.flyway.baseline-on-migrate` to `false`. |
| `backend/src/main/resources/application-docker.yml` | **Modified** | Changed `spring.flyway.baseline-on-migrate` to `false`. |
| `docs/infrastructure/sql/M18_3_1_provision_vc_app_user.sql` | **Created** | Idempotent staging provisioning script for `vc_app_user`, code-backed DML grants, 15 sequence grants, Architecture A runtime RLS policies, and scoped default privileges (parameterized password via `vc.app_user_password`; zero hardcoded secrets). |
| `docs/infrastructure/sql/M18_3_2_revoke_app_object_acls.sql` | **Created** | Idempotent object-by-object `REVOKE ALL` script strictly enumerating the 22 `postgres`-owned tables and 18 `postgres`-owned sequences in `public` for `anon`, `authenticated`, `PUBLIC`, plus `ALTER DEFAULT PRIVILEGES FOR ROLE postgres`. |
| `docs/infrastructure/sql/M18_3_ROLLBACK.sql` | **Created** | Documented emergency rollback script keeping RLS enabled on all 22 tables. |

### 2.3 Automated Test Suite Additions & Updates

| Test File | Action | Coverage Added / Updated |
| :--- | :---: | :--- |
| `backend/src/test/java/com/virtualclassroom/StorageSecurityRemediationTest.java` | **Created** | 11 new security tests verifying official Supabase REST API endpoints/headers (`Authorization` + `apikey` + `x-upsert: false`), no `/object/public/` links, expiry clamping (`60..3600s`), fail-closed `staging` error handling without secret leakage, path traversal rejection, Student/Faculty/Admin `StorageAuthorizationService` ownership enforcement, and local endpoint lockdown. |
| `backend/src/test/java/com/virtualclassroom/StagingConfigurationRemediationTest.java` | **Modified** | Updated staging profile assertions for disabled runtime Flyway, HikariCP leak detection, PostgreSQL JDBC properties, error suppression, and CORS isolation; added production profile isolation test. |
| `backend/src/test/java/com/virtualclassroom/JwtTokenProviderSecurityTest.java` | **Modified** | Added tests confirming `staging` rejects `LOCAL_DEV_FALLBACK_SECRET` and accepts valid 256-bit secrets. |
| `backend/src/test/java/com/virtualclassroom/FullStackIntegrationEndToEndTest.java` | **Modified** | Updated `test06_FileStorageLifecycle_EndToEnd` to prove fail-closed `403` rejection for unlinked faculty uploads requested by a student, `200` for the faculty uploader, and `200` for the enrolled student once linked as a published `StudyMaterial`. |

---

## 3. Exact Database Role Attributes (Live Staging Catalog Evidence)

Verified directly from `pg_roles` and `pg_class` on `virtual-classroom-staging` (`zrbpjcfssbtxxgrjtsca`):

| Attribute (`pg_roles`) | Verified Live Value | Approved Design Target | Status |
| :--- | :---: | :---: | :---: |
| `rolname` | `vc_app_user` | `vc_app_user` | **MATCH** |
| `rolcanlogin` | `true` | `LOGIN` (`true`) | **MATCH** |
| `rolsuper` | `false` | `NOSUPERUSER` (`false`) | **MATCH** |
| `rolbypassrls` | `false` | `NOBYPASSRLS` (`false`) | **MATCH** |
| `rolcreaterole` | `false` | `NOCREATEROLE` (`false`) | **MATCH** |
| `rolcreatedb` | `false` | `NOCREATEDB` (`false`) | **MATCH** |
| `rolinherit` | `false` | `NOINHERIT` (`false`) | **MATCH** |
| `rolconnlimit` | `20` | `CONNECTION LIMIT 20` | **MATCH** |
| Owned objects (`pg_class`) | `0` | `0` (ownership retained by `postgres`) | **MATCH** |

*Credential Handling Note:* A cryptographically random 40-character password was generated via `java.security.SecureRandom` and persisted exclusively into the local gitignored `.env` (`APP_USER_DATABASE_PASSWORD`). It was **never** printed, logged, hardcoded in SQL, or committed to Git.

---

## 4. Actual Privilege Inventory (`vc_app_user`)

### 4.1 Verified Table Grants (`information_schema.role_table_grants`)

| Table (`public.*`) | Verified Privileges for `vc_app_user` | Notes |
| :--- | :--- | :--- |
| `answers` | `DELETE, INSERT, SELECT, UPDATE` | Verified repository `delete()` |
| `assignment_submissions` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `assignments` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `audit_logs` | **`INSERT, SELECT`** | **Strictly append-only (`UPDATE`/`DELETE` denied)** |
| `courses` | `SELECT, UPDATE` | Hibernate `Course.archived` toggle (`INSERT`/`DELETE` withheld) |
| `degrees` | `SELECT` | Read-only curriculum catalog |
| `examination_results` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `examinations` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `faculty_profiles` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `faculty_subject_assignments` | `INSERT, SELECT` | Append/allocate only |
| `flyway_schema_history` | **`NONE`** | **Completely inaccessible to runtime role** |
| `live_sessions` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `notifications` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `password_reset_tokens` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `questions` | `DELETE, INSERT, SELECT, UPDATE` | Verified repository `delete()` |
| `refresh_tokens` | `DELETE, INSERT, SELECT, UPDATE` | Verified repository `deleteByUserId()` |
| `student_profiles` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `student_subject_enrollments` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `study_materials` | `DELETE, INSERT, SELECT, UPDATE` | Verified repository `delete()` |
| `subjects` | `SELECT` | Read-only curriculum catalog |
| `users` | `INSERT, SELECT, UPDATE` | Hard-delete withheld |
| `video_lectures` | `DELETE, INSERT, SELECT, UPDATE` | Verified repository `delete()` |

### 4.2 Verified Sequence Grants (`pg_class.relacl` for `relkind = 'S'`)
- **15 Runtime-Writable Sequences Granted `USAGE, SELECT` (`vc_app_user=rU/postgres`):**
  `answers_id_seq`, `assignment_submissions_id_seq`, `assignments_id_seq`, `audit_logs_id_seq`, `examination_results_id_seq`, `examinations_id_seq`, `live_sessions_id_seq`, `notifications_id_seq`, `password_reset_tokens_id_seq`, `questions_id_seq`, `refresh_tokens_id_seq`, `student_subject_enrollments_id_seq`, `study_materials_id_seq`, `users_id_seq`, `video_lectures_id_seq`.
- **3 Curriculum Catalog Sequences Withheld (0 grants to `vc_app_user`):**
  `courses_id_seq`, `degrees_id_seq`, `subjects_id_seq`.

---

## 5. Actual Post-Remediation ACL Inventory & Preservation Checks

### 5.1 Table ACLs (`pg_class.relacl` — 0 `anon` / `authenticated` Grants)
Verified across all **22** tables in `public` after running `M18_3_2_revoke_app_object_acls.sql` and performing a second full idempotent re-run of both scripts:

```text
answers                        | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arwd/postgres}
assignment_submissions         | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
assignments                    | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
audit_logs                     | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=ar/postgres}
courses                        | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=rw/postgres}
degrees                        | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=r/postgres}
examination_results            | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
examinations                   | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
faculty_profiles               | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
faculty_subject_assignments    | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=ar/postgres}
flyway_schema_history          | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres}
live_sessions                  | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
notifications                  | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
password_reset_tokens          | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
questions                      | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arwd/postgres}
refresh_tokens                 | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arwd/postgres}
student_profiles               | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
student_subject_enrollments    | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
study_materials                | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arwd/postgres}
subjects                       | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=r/postgres}
users                          | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arw/postgres}
video_lectures                 | RLS=true | ACL={postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=arwd/postgres}
```

### 5.2 Scoped `pg_default_acl` & Preserved Supabase-Managed Objects
- **`pg_default_acl` (`public`):**
  - `role=postgres | type=r (tables)` -> `{postgres=arwdDxtm/postgres,service_role=arwdDxtm/postgres,vc_app_user=r/postgres}` (`anon` and `authenticated` removed; conservative `SELECT`-only default for `vc_app_user`).
  - `role=postgres | type=S (sequences)` -> `{postgres=rwU/postgres,service_role=rwU/postgres,vc_app_user=rU/postgres}` (`anon` and `authenticated` removed).
  - `role=postgres | type=f (routines)` -> **Preserved untouched.**
  - `role=supabase_admin | type=r/S/f` -> **Preserved 100% untouched.**
- **Preserved Objects Verified:**
  - `public.rls_auto_enable()` (`owner=postgres`) — **PRESERVED**.
  - `storage.buckets` (`academic-assets`, `public=false`, `file_size_limit=52428800`) — **PRESERVED**.
  - RLS (`relrowsecurity = true`) on all **22** tables in `public` — **PRESERVED**.

---

## 6. RLS Configuration & Honest Security Disclosure (Architecture A)

- **Active Policies (`pg_policies` in `public`):** Exactly **21** policies, named `vc_app_user_runtime_access` (`FOR ALL TO vc_app_user USING (true) WITH CHECK (true)`), applied exclusively to the 21 application tables.
- **Excluded Tables / Roles:**
  - `public.flyway_schema_history`: **0 policies** (and 0 grants to `vc_app_user`).
  - `anon` and `authenticated`: **0 policies** (and 0 table/sequence grants).
- **Honest Architecture A Security Boundary:**
  - Permissive `USING (true) WITH CHECK (true)` policies for `vc_app_user` **do NOT provide per-user, per-student, or per-faculty row-level isolation at the PostgreSQL layer**.
  - **Spring Security (`SecurityConfig`, `JwtAuthenticationFilter`, `@PreAuthorize`) and service-layer domain checks (`StudentService`, `FacultyService`, `AssignmentService`, `StorageAuthorizationService`) remain the authoritative per-user authorization boundary.**
  - Running `vc_app_user` with `NOBYPASSRLS` ensures PostgreSQL still enforces RLS on every table (proven live via temporary probe table `public._m18_3_rls_probe` containing 1 row with RLS enabled and 0 policies: `vc_app_user` observed `0` rows) and blocks access to `flyway_schema_history` or any future un-policied table.

---

## 7. Live Pooler Authentication, Privilege Gate & Hibernate Evidence

### 7.1 Supavisor Pooler Connection & TLS Proof
- **Connection Target:** `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
- **Tenant-Qualified Username:** `vc_app_user.zrbpjcfssbtxxgrjtsca`
- **Verified Identity:** `current_user = vc_app_user` | `current_database = postgres`
- **Client -> Supavisor TLS:** `Verified = true` | `Protocol = TLSv1.3` | `Cipher = TLS_AES_256_GCM_SHA384`
- **Supavisor -> Local PG Hop:** `pg_stat_ssl = false` (expected Supavisor pooler edge TLS termination).

### 7.2 Negative & Positive Privilege Verification (Live Staging)
- **Negative Checks (13/13 DENIED as required):**
  - `CREATE TABLE public._m18_3_bad_ddl` -> `ERROR: permission denied for schema public`
  - `ALTER TABLE public.users` -> `ERROR: must be owner of table users`
  - `DROP TABLE public.users` -> `ERROR: must be owner of table users`
  - `TRUNCATE TABLE public.notifications` -> `ERROR: permission denied for table notifications`
  - `CREATE ROLE` -> `ERROR: permission denied to create role`
  - `CREATE DATABASE` -> `ERROR: permission denied to create database`
  - `SELECT public.flyway_schema_history` -> `ERROR: permission denied for table flyway_schema_history`
  - `DELETE public.users` -> `ERROR: permission denied for table users`
  - `DELETE public.assignment_submissions` -> `ERROR: permission denied for table assignment_submissions`
  - `DELETE public.examination_results` -> `ERROR: permission denied for table examination_results`
  - `INSERT public.degrees` -> `ERROR: permission denied for table degrees`
  - `UPDATE public.audit_logs` -> `ERROR: permission denied for table audit_logs`
  - `DELETE public.audit_logs` -> `ERROR: permission denied for table audit_logs`
- **Positive DML Checks (6/6 PASSED inside rolled-back TX; sequences cleanly restored):**
  - `SELECT public.degrees` (`5` rows), `public.courses` (`9` rows), `public.subjects` (`8` rows)
  - `UPDATE public.courses SET archived = archived WHERE id = 1`
  - `INSERT INTO public.audit_logs (action, target_entity, details) ...`
  - `INSERT` + `UPDATE` on `public.users`
- **NOBYPASSRLS Proof:** Temporary probe table (`1` row, RLS=true, 0 policies, `SELECT` granted to `vc_app_user`) returned **`0` rows** to `vc_app_user`, then dropped cleanly.
- **HikariCP & Hibernate Verification as `vc_app_user`:**
  - **10/10 concurrent pooled queries** succeeded with 0 leaks (`vc_app_user:8` subjects verified).
  - **Hibernate 6.6.8 `ddl-auto: validate` PASSED 100%** across all **21 JPA entities** in `5.191s` (`users=0`, `audit_logs=0`, `degrees=5`, `courses=9`, `subjects=8`).

---

## 8. Storage Security Verification

- **Live Bucket Inspection (`storage.buckets`):** Verified `academic-assets` (`public = false`, `file_size_limit = 52428800`, 14 exact `allowed_mime_types`). `FileStorageService.ALLOWED_MIME_TYPES` was reconciled 1:1 to those exact 14 MIME types (removing `image/gif`, `application/zip`, and `application/x-zip-compressed`).
- **Automated Protocol & Authorization Verification:**
  - `StorageSecurityRemediationTest` (11/11 passed): Proved exact `POST /storage/v1/object/sign/{bucket}/{path}` and `POST /storage/v1/object/upload/sign/{bucket}/{path}` HTTP calls with both `Authorization: Bearer` and `apikey` headers, `x-upsert: false`, expiry clamping (`60..3600s`), zero `/object/public/` URLs, fail-closed `StorageOperationException` without key leakage, path traversal rejection, Student/Faculty/Admin ownership authorization, and `403 Forbidden` on `/api/v1/storage/files/**` in cloud mode.
  - `FullStackIntegrationEndToEndTest.test06_FileStorageLifecycle_EndToEnd`: Proved end-to-end `403` rejection of unauthorized student download for unlinked faculty upload, `200` for faculty uploader, and `200` for enrolled student after material publication.
- **Honest Verification Scope Note:** Live HTTP calls to `https://zrbpjcfssbtxxgrjtsca.supabase.co/storage/v1/...` were verified via `MockRestServiceServer` because `SUPABASE_SERVICE_ROLE_KEY` is intentionally not stored in the local `.env` and will be injected as a Render secret during staging deployment. Live round-trip upload/signed-download against `academic-assets` should be executed as the first post-deploy smoke test once Render staging is configured.

---

## 9. Backend Test & GitHub Actions CI Results

### 9.1 Local Backend Maven Suite (`./mvnw test`)
- **Total Tests:** `140`
- **Passed:** `134`
- **Failed:** `0`
- **Errors:** `0`
- **Skipped:** `6` *(never counted as passed)*
  - Exact skipped tests & reason (Windows host OS error=623 blocking `embedded-pg` `initdb.exe`):
    1. `FlywayMigrationSafetyTest.scenarioA_FreshPostgresDatabase_AppliesAllMigrationsCleanly`
    2. `FlywayMigrationSafetyTest.scenarioB_ExistingPostgresWithLegitimateData_PreservedAfterV4`
    3. `StandaloneFlywayMigrationRunnerTest.testSuccessfulMigrationAgainstIsolatedDatabase`
    4. `StandaloneFlywayMigrationRunnerTest.testIdempotentSecondExecution`
    5. `StandaloneFlywayMigrationRunnerTest.testMigrationFailureReporting`
    6. `StandaloneFlywayMigrationRunnerTest.testHibernateSchemaCompatibilityNonDeployment`
  - All 6 PostgreSQL 17 integration tests executed and **PASSED** on Linux in GitHub Actions CI (`PostgreSQL 17 Migration & Standalone Runner Gate`).

### 9.2 GitHub Actions CI Verification (Run `44249038808` — Commit `7cc23ac`)
Workflow URL: `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/44249038808`

| CI Job | Status | Conclusion | Notes |
| :--- | :---: | :---: | :--- |
| **PostgreSQL 17 Migration & Standalone Runner Gate** | Completed | **`SUCCESS`** | All V1–V5 migrations & Standalone Runner verified against `postgres:17-alpine`. |
| **Backend QA (JUnit 5, Mockito, Spring Security, Package)** | Completed | **`SUCCESS`** | All backend tests including new Milestone 18.3 security suites passed and JAR packaged cleanly. |
| **Docker Image Build & Compose Orchestration** | Completed | **`SUCCESS`** | Multi-stage unprivileged container image built and Compose orchestration verified. |
| **Frontend QA (TypeScript, Vitest, Build, Audit)** | Completed | **`FAILURE`** | TypeScript production build (`SUCCESS`) and Vitest unit/contract tests (`SUCCESS`) passed; Step 8 (`Run npm security audit`) failed due to an upstream npm advisory unrelated to Milestone 18.3 backend/DB changes. |

---

## 10. Remaining Risks & Follow-Up Items

1. **Architecture A Database-Layer Row Isolation (Accepted Trade-Off):** `vc_app_user` uses permissive runtime RLS policies (`USING (true)`); per-user isolation depends strictly on Spring Security and service layers. Architecture B (`set_config` transaction-scoped RLS) remains available as a future defense-in-depth upgrade without changing roles or connection strings.
2. **Live Supabase Storage Smoke Test Pending Render Secrets:** Execute one live upload + signed-download + expired-URL check against `academic-assets` immediately after injecting `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` into Render staging.
3. **Frontend `npm security audit` Advisory (Unrelated to M18.3):** Address the upstream frontend npm advisory in a dedicated frontend dependency maintenance commit before merging `feature/staging-readiness-config` into `develop`.
4. **Supabase Staging Micro-Instance Transient Restarts:** During Phase 6 verification, Supabase's staging micro-instance underwent a brief maintenance bounce (`{:error, :econnrefused}` / `EAUTHQUERY`) before recovering cleanly. Render's `/actuator/health/liveness` probe (which does not kill the container on transient DB blips) and HikariCP connection validation mitigate this behavior safely.
5. **Production Rollout Pending Separate Authorization:** Production (`tzbjmxtmysvzxdmspiui`) has **not** been provisioned with `vc_app_user` or ACL revocations yet and remains completely untouched.

---

## 11. Rollback Instructions

If an unforeseen runtime regression occurs during future Render staging verification:
1. **Immediate Render Rollback (< 2 minutes):**
   - In Render Staging Dashboard, set `DATABASE_USERNAME` to `postgres.zrbpjcfssbtxxgrjtsca` and `DATABASE_PASSWORD` to the staging `postgres` secret.
   - Trigger a Render redeploy and confirm `/actuator/health/liveness` returns `UP`.
2. **Database ACL & Policy Rollback (Optional — Only If Needed):**
   - Execute `docs/infrastructure/sql/M18_3_ROLLBACK.sql` against Supabase staging as `postgres`.
   - RLS remains **ENABLED** on all 22 public tables even after rollback, preserving PostgREST deny-all protection for `anon` and `authenticated`.

---

## 12. Operator Handoff

Milestone 18.3 staging security implementation and database verification are **COMPLETE**. No Render or Vercel deployment has been initiated, and `develop`/`main` branches remain untouched.

**Ready for Operator Review prior to Render Staging Secret Configuration and Deployment Authorization.**
