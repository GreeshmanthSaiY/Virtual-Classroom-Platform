# Virtual Classroom Platform — Database Migration Strategy & Lifecycle

## 1. Migration History & Lineage

The database evolution is strictly managed via **Flyway**, ensuring identical schema state across local development, disposable test harnesses, continuous integration, and production Supabase environments.

The canonical migration directory is:
`backend/src/main/resources/db/migration/`

| Version | Migration Script | Purpose | Idempotency / Safety Notes |
|:---|:---|:---|:---|
| **V1** | `V1__init_schema.sql` | Establishes foundational schema: `users`, `student_profiles`, `faculty_profiles`, `courses`, `subjects`, `study_materials`, `recorded_lectures`, `qa_threads`, `qa_replies`, `notifications`, `audit_logs`. | Strict DDL, foreign keys, cascade rules. |
| **V2** | `V2__seed_academic_data.sql` | Seeds foundational degree programs (B.Tech CS, IT, ECE) and initial subjects. | Seeds reference data without hardcoding production credentials. |
| **V3** | `V3__security_harden_remove_demo_users.sql` | Purges demo users from production baseline, synchronizes sequence counters, adds foreign key ON DELETE rules. | Safe targeted deletion preserving real non-demo records. |
| **V4** | `V4__academic_platform_expansion.sql` | Adds `refresh_tokens`, `live_sessions`, `assignments`, `assignment_submissions`, `examinations`, `examination_results`, `student_subject_enrollments`, and enhances profiles & subjects. | Incremental non-destructive expansion, backfills enrollments, creates unique constraints and indexes. |

---

## 2. Inviolable Migration Rules

1. **Applied Migrations Are Strictly Immutable**:
   - Once a migration (e.g. V1, V2, V3) is committed and applied, its SQL contents and file checksum **must never be modified**.
   - Any schema correction or alteration must be introduced as a new forward migration (`V5__...`).
2. **Zero Inlined Demo Credentials**:
   - Production migrations must never contain default credentials, plaintext passwords, or hardcoded administrative accounts.
   - Initial administrative provisioning must occur via environment-controlled bootstrap scripts or seed services during first startup.
3. **Additive, Backward-Compatible DDL (Expand-Contract Pattern)**:
   - When modifying tables in production:
     - **Phase 1 (Expand)**: Add new nullable columns or columns with sensible defaults (`ALTER TABLE ... ADD COLUMN ... DEFAULT ...`).
     - **Phase 2 (Migrate Data)**: Backfill data asynchronously or via update statements.
     - **Phase 3 (Enforce Constraints)**: Apply `NOT NULL` or `UNIQUE` constraints once all records are compliant.
     - **Phase 4 (Contract)**: Drop deprecated columns only after all backend consumers are updated.

---

## 3. Automated Dual-Scenario Testing

Every pull request and build validates Flyway migrations against real disposable PostgreSQL containers/instances via `FlywayMigrationSafetyTest.java`:

- **Scenario A (Fresh Database Initialization)**:
  - An empty PostgreSQL instance is initialized.
  - Flyway runs migrations V1, V2, V3, and V4 from scratch.
  - Validates that a brand new deployment arrives at the exact target schema without missing dependencies.
- **Scenario B (Incremental Upgrade from V3)**:
  - An empty PostgreSQL instance is initialized with V1, V2, and V3.
  - Non-demo academic records (students, faculty, subjects) are inserted.
  - Flyway executes `V4__academic_platform_expansion.sql`.
  - Validates that:
    - Existing records remain completely intact.
    - Enrollment backfills correctly preserve student academic history.
    - Checksums and schema versions match expectation.

---

## 4. Failure Recovery & Repair Procedures

In the event of a failed migration execution (e.g., syntax error, connection timeout):

1. **Flyway Repair**:
   If a migration failed in development and left a broken state in `flyway_schema_history`:
   ```bash
   ./mvnw.cmd flyway:repair
   ```
   *Note: Never execute repair in production without first manually auditing the database state.*
2. **Forward Remediation**:
   If a migration succeeded partially in a non-transactional DDL environment:
   - Create a targeted forward migration `V<N+1>__fix_...sql` that checks for column existence via `IF NOT EXISTS` before executing.
