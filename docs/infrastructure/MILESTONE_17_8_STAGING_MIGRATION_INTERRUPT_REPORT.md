# Milestone 17.8 — Staging Migration Execution Discrepancy & Safe Recovery Report

**Target Project:** `virtual-classroom-staging`  
**Project Reference:** `zrbpjcfssbtxxgrjtsca`  
**Region:** `ap-south-1` (Mumbai, India)  
**Host:** `aws-0-ap-south-1.pooler.supabase.com:5432`  
**Database:** `postgres`  
**User:** `postgres` (via tenant `postgres.zrbpjcfssbtxxgrjtsca`)  
**PostgreSQL Version:** `17.11 on aarch64-unknown-linux-gnu`  
**Execution Timestamp:** 2026-11-16 18:19:44 IST  
**Status:** **PAUSED AT SAFETY GATE (UNEXPECTED MIGRATION HISTORY STATE DETECTED)**

---

## 1. Executive Summary

During the initial execution of the Standalone Flyway Migration Runner against the live Supabase staging database, Flyway's `baselineOnMigrate(true)` setting caused Flyway to erroneously baseline the database at Version 1 without executing `V1__init_schema.sql`. Consequently, when Flyway attempted to apply `V2__seed_academic_data.sql`, the migration failed because the application tables defined in `V1` (`degrees`, `courses`, `users`, etc.) did not exist.

In strict compliance with the **Master Execution Directive**:
> *"If any migration fails: Stop immediately. Capture the exact error without revealing secrets. Inspect the Flyway history and actual schema. Do not blindly rerun, repair, clean, or delete migration history. Determine the safe recovery procedure and report it before proceeding."*  
> *"Only pause when: A safety check detects an unexpected database state. A destructive or irreversible action outside the defined migration sequence would be required."*

Execution was immediately paused. A read-only inspection was executed against the live staging database to capture the exact state.

---

## 2. Live Database Evidence (Captured via Authenticated Read-Only Query)

| Inspection Item | Value Observed on Live Database | Implication |
|---|---|---|
| **Database Name** | `postgres` | Matches target specification |
| **Connected User** | `postgres` (pooler routing verified) | Matches target specification |
| **PostgreSQL Version** | `17.11 on aarch64-unknown-linux-gnu` | Supported PostgreSQL 17 engine |
| **Public Application Tables** | **0** | No application tables exist; zero user data exists |
| **Total Public Tables** | **1** (`flyway_schema_history`) | Only Flyway's metadata table was created |
| **Users Table Exists** | `false` | Confirmed absent |
| **Flyway Table State** | 1 baseline row (Version 1) | Caused Flyway to skip `V1` |

### Exact Rows in `public.flyway_schema_history`
```
Rank: 1
Version: '1'
Description: '<< Flyway Baseline >>'
Type: 'BASELINE'
Script: '<< Flyway Baseline >>'
Checksum: null
Success: true
Execution Time: 0ms
Installed On: 2026-11-16 12:47:45.103022
```

---

## 3. Root Cause Analysis

1. **`baselineOnMigrate(true)` Configuration**:
   `StandaloneFlywayMigrationRunner.java` had `.baselineOnMigrate(true)` configured (carried over from Spring Boot's `application-staging.yml`).
2. **Supabase Environment Schema Detection**:
   In managed Supabase PostgreSQL, default system objects, extensions, and roles exist in the database environment. Flyway's `baselineOnMigrate(true)` algorithm detected these existing database objects and determined that the target schema was non-empty.
3. **Premature Baselining at Version 1**:
   Flyway automatically executed `DbBaseline`, creating `flyway_schema_history` and recording Version 1 as `BASELINE`.
4. **V1 Skipped, V2 Failed**:
   Flyway marked `V1__init_schema.sql` as `BASELINE_IGNORED` and immediately attempted to execute `V2__seed_academic_data.sql`. V2 failed with:
   ```
   ERROR: relation "degrees" does not exist
   Position: 112
   Location: db/migration/V2__seed_academic_data.sql
   Line: 4
   ```
5. **Clean Rollback**:
   Because Flyway executes migrations within individual transactions, V2 rolled back completely. No partial tables or orphaned rows were created.

---

## 4. Safe Recovery Procedure

To allow the canonical migrations `V1` through `V5` to execute from scratch:

1. **Step 1 — Change Configuration to `baselineOnMigrate(false)`**:
   Modify `StandaloneFlywayMigrationRunner.java` line 130 to:
   ```java
   .baselineOnMigrate(false)
   ```
   This guarantees Flyway will never baseline the schema and will execute `V1__init_schema.sql` directly on an empty database.

2. **Step 2 — Drop Erroneous Baseline Table `public.flyway_schema_history`**:
   Because no application tables or application data exist in the database (0 application tables confirmed), dropping the single metadata table `public.flyway_schema_history` restores the database to a completely pristine state.
   ```sql
   DROP TABLE public.flyway_schema_history;
   ```

3. **Step 3 — Execute Migrations V1 through V5**:
   Execute `StandaloneFlywayMigrationRunner.runForStaging()`.
   Flyway will:
   - Create a clean `public.flyway_schema_history`.
   - Apply `V1__init_schema.sql` (creating 13 initial tables, PKs, FKs, indexes).
   - Apply `V2__seed_academic_data.sql` (inserting canonical degrees, courses, subjects, demo seed).
   - Apply `V3__security_harden_remove_demo_users.sql` (purging demo accounts, resetting sequences).
   - Apply `V4__academic_platform_expansion.sql` (creating 7 expansion tables).
   - Apply `V5__password_reset_and_course_archival.sql` (creating password reset tokens, archival columns).
   - Execute all post-migration integrity assertions.

4. **Step 4 — Post-Migration Audit**:
   Verify:
   - 5 migrations in `public.flyway_schema_history` with `success = true`.
   - 21 application tables in `public`.
   - 25 custom indexes (`idx_*`).
   - Reference curriculum (5 degrees, 9 courses, 8 subjects).
   - 0 demo users in `users`.
   - Clean sequence resets (`users_id_seq`, `questions_id_seq`).
