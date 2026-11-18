# STAGING SECURITY VERIFICATION REPORT
## Virtual Classroom Platform — Database Identity, RLS, Storage, CI Audit & Workflow Safeguards

**Verification Date:** 2026-11-18  
**Supabase Staging Project:** `virtual-classroom-staging` (`zrbpjcfssbtxxgrjtsca`, `ap-south-1`)  
**Production Supabase (`tzbjmxtmysvzxdmspiui`):** **STRICTLY UNTOUCHED**

---

## 1. Live Staging Supabase Database Security Verification (Phase D — Executed Live)

Executed directly against `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require` using runtime identity `vc_app_user.zrbpjcfssbtxxgrjtsca` (password loaded from local gitignored `.env`, never logged or printed):

| Security Verification Check | Live Observed Output | Status |
| :--- | :--- | :---: |
| **TLS Transport Enforcement** | Connected with `sslmode=require` | **PASS** |
| **PostgreSQL Engine Version** | `PostgreSQL 17.11 on aarch64-unknown-linux-gnu` | **PASS** |
| **Authenticated Runtime Role** | `current_user = vc_app_user` (NOT `postgres`) | **PASS** |
| **Least-Privilege Role Attributes** | `rolsuper=false`, `rolcanlogin=true`, `rolcreatedb=false`, `rolcreaterole=false`, `rolinherit=false`, `rolbypassrls=false`, `rolconnlimit=20` | **PASS** |
| **RLS Coverage Across Public Schema** | `22/22` public tables have `rowsecurity = true` *(Note: per M18.2.1 architecture, `USING(true)` app policies enforce non-public isolation while Spring Security & backend services remain authoritative for per-user RBAC)* | **PASS** |
| **Seed & Zero-Demo-User Integrity** | `degrees=5`, `courses=9`, `subjects=8`, `users=0` (zero demo accounts) | **PASS** |
| **Negative Test: Flyway Metadata Read** | `SELECT count(*) FROM public.flyway_schema_history` -> **DENIED (`SQLSTATE 42501`)** | **PASS** |
| **Negative Test: Unauthorized DDL** | `CREATE TABLE public._m19_4_forbidden_probe` -> **DENIED (`SQLSTATE 42501`)** | **PASS** |
| **Negative Test: Direct `storage.buckets` Read** | `SELECT ... FROM storage.buckets` -> **DENIED (`SQLSTATE 42501`)** | **PASS** |
| **Permitted Application DML (Rollback-Only)** | Transactional `INSERT INTO public.audit_logs ... RETURNING id` succeeded (`id=1`) and cleanly rolled back (`remaining=0`) | **PASS** |
| **Private Storage Bucket Public Probe** | `GET https://zrbpjcfssbtxxgrjtsca.supabase.co/storage/v1/object/public/academic-assets/nonexistent-probe.txt` -> **HTTP `400` (`NoSuchBucket`)** | **PASS** |

---

## 2. Application, Bundle & Dependency Security Verification

| Security Domain | Verification Method | Result | Status |
| :--- | :--- | :--- | :---: |
| **Backend Storage Authorization & Fail-Closed Behavior** | `StorageSecurityRemediationTest` (`11/11`) + source inspection (`FileStorageService`, `StorageAuthorizationService`, `StorageController`, `SecurityConfig`) | Signed URLs via Supabase REST API (default `900s`), path canonicalization, MIME allowlists, ownership RBAC, fail-closed in `staging`/`prod`, `/api/v1/storage/files/**` blocked (`403`) when cloud storage active | **PASS (Unit/Code)** |
| **JWT Secret & Cookie Hardening** | `JwtTokenProviderSecurityTest` (`6/6`) + `AuthController` + `application-staging.yml` | Staging rejects fallback dev JWT secret; `HttpOnly`, `Secure=true`, `SameSite=None` refresh cookie; pure in-memory access token on frontend | **PASS (Unit/Code)** |
| **Staging Error Leakage Suppression** | `application-staging.yml` & `application-prod.yml` | `server.error.include-message: never`, `include-binding-errors: never` | **PASS (Config)** |
| **Staging CORS Policy** | `render.yaml` & `application-staging.yml` | Restricted to `https://virtual-classroom-staging.vercel.app` (`localhost` removed); live preflight positive/negative verification remains `BLOCKED` pending Render deploy | **PASS (Config) / BLOCKED (Live)** |
| **Frontend Build Artifact Secret Scan** | Regex scan across all `47` compiled files in `frontend/dist` | `0` `localhost` URLs, `0` `service_role` keys, `0` Supabase refs, `0` backend secrets | **PASS** |
| **Frontend npm Dependency Security Policy (M18.4)** | GitHub Actions Run `44321518786` (`Frontend QA`) | Blocking prod audit (`npm audit --omit=dev --audit-level=high`): **`0` HIGH/CRITICAL (PASS)**. Full audit visibly reports tracked unpatched dev-only `GHSA-fc5q-39xc-vjpf` / `CVE-2026-112366` (`picomatch <= 4.0.7`); moderate `react-router-dom@6.30.6` and `@vitest/mocker@3.2.7` tracked separately. | **PASS (Policy Enforced)** |
| **Admin Bootstrap Security (Phase H)** | `StagingAdminBootstrapRunnerTest` (`7/7`) | `@Profile("staging")` only, runtime `prod` fail-closed guard, disabled by default, requires external strong password (>= 14 chars, 4 classes), rejects demo emails, idempotent, zero credential logging | **PASS (Unit/Code)** |
| **Live Negative API Security Tests (Expired/Missing/Wrong-Role JWT, Live CORS, Live Privilege Escalation)** | Requires deployed Render staging service | Cannot execute against live Render URL until service is provisioned and deployed | **BLOCKED** |

---

## 3. CI/CD & GitHub Deployment Safety Verification

- **Manual-Only Staging Workflow:** `.github/workflows/staging-deployment.yml` uses `on: workflow_dispatch` exclusively (zero `push` triggers). Verified `0` staging deployments triggered on PR #6 merge to `develop`.
- **Dual Confirmation + Safe Defaults:** `dry_run_only: true`, `confirm_live_deployment: false`; live deploy requires both flags explicitly inverted.
- **Branch & Reviewer Enforcement:** Live GitHub Environment `staging` enforces `required_reviewers: GreeshmanthSaiY`, `can_admins_bypass: false`, and branch restriction strictly to `develop`.
- **Production Isolation:** `main` (`3632edddd1d5784c41ec9791520afc5c41400611`), `production-release.yml`, and production Supabase (`tzbjmxtmysvzxdmspiui`) were never modified or accessed.
