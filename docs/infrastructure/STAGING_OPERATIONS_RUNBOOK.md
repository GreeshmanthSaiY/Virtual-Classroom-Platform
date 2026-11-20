# STAGING OPERATIONS RUNBOOK
## Virtual Classroom Platform — Cloud Service Operations, Monitoring, Troubleshooting & Rollback

**Repository:** `GreeshmanthSaiY/Virtual-Classroom-Platform`  
**Staging Target Branch:** `develop`  
**Authoritative Deployment Mode:** Manual, Gated, Dual-Confirmed via GitHub Actions (`.github/workflows/staging-deployment.yml`)  
**Production Isolation:** Production (`main`) is strictly separate and isolated; production services must never be touched during staging operations.

---

## 1. Staging Architecture & Resource Topology

| Component | Target Infrastructure | Provider / Project | Critical Attributes |
| :--- | :--- | :--- | :--- |
| **Relational Database** | PostgreSQL 17.11 | Supabase (`virtual-classroom-staging`, `zrbpjcfssbtxxgrjtsca`) | Session pooler (`aws-0-ap-south-1.pooler.supabase.com:5432`), TLS (`sslmode=require`), `vc_app_user` runtime role (`NOBYPASSRLS`, limit 20), RLS on all 22 tables. |
| **Object Storage** | Private Supabase Bucket | Supabase Storage (`academic-assets`) | Private bucket (`public = false`), 50 MB max file size, MIME allowlist, short-lived signed URLs (default 900s), server-side ownership enforcement. |
| **Backend Service** | Docker Container | Render (`virtual-classroom-backend-staging`) | Java 21 Spring Boot, `autoDeploy: false`, branch `develop`, health check `/actuator/health/liveness`, port 8080. |
| **Frontend Application** | Static SPA (Vite) | Vercel (`virtual-classroom-staging`) | React + TypeScript, client-side routing rewrites (`vercel.json`), branch `develop`, `withCredentials: true`, in-memory token storage. |
| **CI/CD Pipeline** | GitHub Actions | GitHub Repository | Manual `workflow_dispatch` only, dual confirmation (`dry_run_only=false` + `confirm_live_deployment=true`), Environment `staging` approval gate. |

---

## 2. Deployment Workflows & Operational Execution

### 2.1 Pre-Deployment Dry-Run Verification (Mandatory Safe Step)
Before any live staging deployment, perform a dry-run to verify GitHub Environment secrets and approval gates without triggering any cloud deployments:

1. Navigate to: **GitHub Actions → `Staging Deployment` → Run workflow**.
2. Select branch: **`develop`**.
3. Inputs:
   - `dry_run_only`: **`true`**
   - `confirm_live_deployment`: **`false`**
   - `skip_smoke_tests`: **`false`**
4. Click **Run workflow**.
5. When the GitHub Environment `staging` review gate is presented, review and approve.
6. Verify output:
   - `staging-preflight` succeeds and sets `is_dry_run=true`, `allow_live_deploy=false`.
   - `deploy-staging` is skipped.
   - `dry-run-smoke-tests` runs and validates all 9 smoke test specifications.
   - **Zero** HTTP calls to Render or Vercel are dispatched.

### 2.2 Live Staging Deployment Execution
To perform an authorized live staging deployment:

1. Navigate to: **GitHub Actions → `Staging Deployment` → Run workflow**.
2. Select branch: **`develop`**.
3. Inputs:
   - `dry_run_only`: **`false`** *(explicitly uncheck or set false)*
   - `confirm_live_deployment`: **`true`** *(explicitly check or set true)*
   - `skip_smoke_tests`: **`false`**
4. Click **Run workflow**.
5. Promptly approve the **GitHub Environment `staging` reviewer gate** (`GreeshmanthSaiY`).
6. The automated workflow executes:
   - **Preflight:** Validates `refs/heads/develop`, validates dual confirmation, and validates presence of all 5 staging secrets.
   - **Render Deploy:** Triggers `RENDER_STAGING_DEPLOY_HOOK_URL` (HTTP 200/201).
   - **Vercel Deploy:** Runs `vercel pull`, `vercel build`, `vercel deploy --prebuilt`.
   - **Rollout Polling:** Polls `${BACKEND_URL}/actuator/health/liveness` every 15s (up to 8 minutes) until HTTP 200 UP is received.
   - **Smoke Tests:** Dispatches live HTTP contract tests against target frontend and backend URLs.

---

## 3. One-Time Initial Administrator Bootstrap (Phase H)

Staging databases initially contain zero user accounts (`COUNT(users) = 0`). The application includes an opt-in, staging-only bootstrap runner: `com.virtualclassroom.bootstrap.StagingAdminBootstrapRunner`.

### 3.1 Security Invariants
- **Profile Restriction:** Strictly annotated with `@Profile("staging")` and runtime-guarded to throw `IllegalStateException` if `prod` or `production` is active.
- **Default Disabled:** Will not run unless `STAGING_ADMIN_BOOTSTRAP_ENABLED=true`.
- **Complexity Requirements:** Password must be at least 14 characters long and include uppercase, lowercase, numeric, and special characters.
- **Prohibited Demo Emails:** Rejects `admin@virtualclassroom.edu`, `dr.jeevan@virtualclassroom.edu`, and `gaurav.shukla@virtualclassroom.edu`.
- **Idempotency:** If any user with `ROLE_ADMIN` exists or the target email already exists, the runner logs a skipped message and performs zero database changes.
- **Audit Logging:** Logs `ADMIN_BOOTSTRAP_CREATED` into `audit_logs` table.

### 3.2 Step-by-Step Operator Bootstrap Procedure
Execute this procedure **once** after the initial Render backend deployment succeeds:

1. Open **Render Dashboard → `virtual-classroom-backend-staging` → Environment**.
2. Add / update these environment variables:
   - `STAGING_ADMIN_BOOTSTRAP_ENABLED` = `true`
   - `STAGING_ADMIN_EMAIL` = `<your-operator-admin-email>`
   - `STAGING_ADMIN_FULL_NAME` = `Staging Administrator`
   - `STAGING_ADMIN_PASSWORD` = `<strong-unique-14+-character-password>`
3. Click **Save Changes** and trigger a manual redeploy/restart.
4. Watch Render deployment logs for:
   `Staging administrator bootstrap completed for <email>. Disable STAGING_ADMIN_BOOTSTRAP_ENABLED and unset STAGING_ADMIN_PASSWORD.`
   *(Confirm that zero password characters appear in the logs).*
5. **Immediately return to Render Dashboard:**
   - Set `STAGING_ADMIN_BOOTSTRAP_ENABLED` = `false`.
   - Delete / clear `STAGING_ADMIN_PASSWORD`.
   - Click **Save Changes**.
6. Verify login at `https://virtual-classroom-staging.vercel.app/login` using the bootstrapped administrator email and password.

---

## 4. Monitoring & Health Probes

### 4.1 Backend Endpoints
- **Liveness Probe:** `GET /actuator/health/liveness`
  - Expected Response: `HTTP 200 {"status":"UP"}`
  - Used by Render container health checks and GitHub Actions rollout polling.
- **Readiness / Component Health:** `GET /actuator/health`
  - Expected Response: `HTTP 200 {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}`
- **Security Barrier Verification:** `GET /api/v1/admin/metrics`
  - Expected Unauthenticated Response: `HTTP 401 Unauthorized` or `HTTP 403 Forbidden` (validates Spring Security filter chain is active).
- **Storage Local File Barrier:** `GET /api/v1/storage/files/probe.txt`
  - Expected Response: `HTTP 401` or `HTTP 403` (verifies local file streaming is disabled in cloud staging).

### 4.2 Manual Smoke Test Execution
To run smoke tests locally against deployed staging services without re-running CI:
```bash
node scripts/staging_smoke_tests.js \
  --frontend="https://virtual-classroom-staging.vercel.app" \
  --backend="https://virtual-classroom-backend-staging.onrender.com"
```

---

## 5. Troubleshooting & Diagnostics

### 5.1 Database Connectivity / HikariCP Pool Failures
- **Symptom:** Backend container fails startup with `Connection refused`, `UnknownHostException`, or `HikariPool - Exception during pool initialization`.
- **Root Causes & Remediations:**
  1. `DATABASE_URL` format: Must use the Supabase session pooler on port 5432: `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`. Direct port 6543 (transaction pooler) does not support session prepared statements.
  2. `DATABASE_USERNAME` format: Must be tenant-qualified: `vc_app_user.zrbpjcfssbtxxgrjtsca`. Unqualified `vc_app_user` is rejected by Supavisor.
  3. `DATABASE_PASSWORD`: Must match the 40-character password provisioned for `vc_app_user` (from local gitignored `.env`).

### 5.2 CORS & Authentication Failures
- **Symptom:** Browser console shows `CORS error`, `Access-Control-Allow-Origin missing`, or `Set-Cookie` blocked.
- **Root Causes & Remediations:**
  1. `CORS_ALLOWED_ORIGINS` in Render must match the Vercel staging URL **exactly** (e.g., `https://virtual-classroom-staging.vercel.app` with no trailing slash and no `localhost`).
  2. `COOKIE_SECURE`: Must be `true` on HTTPS.
  3. `COOKIE_SAME_SITE`: Must be `None` to permit cross-site cookie transport between Render (`.onrender.com`) and Vercel (`.vercel.app`).

### 5.3 Supabase Private Storage Failures
- **Symptom:** Upload or signed URL download fails with `502 Bad Gateway` or `StorageOperationException`.
- **Root Causes & Remediations:**
  1. `SUPABASE_URL`: Must be `https://zrbpjcfssbtxxgrjtsca.supabase.co`.
  2. `SUPABASE_SERVICE_ROLE_KEY`: Must be the staging project's service-role key (from Supabase Dashboard → Settings → API). Never use the anon key or production service-role key.
  3. `SUPABASE_STORAGE_BUCKET`: Must be `academic-assets`.
  4. MIME Type Restrictions: Staging bucket enforces allowed document and media MIME types (`application/pdf`, `image/jpeg`, `image/png`, etc.) and a 50 MB file size limit.

---

## 6. Rollback Procedures

### 6.1 Instant Frontend Rollback (Vercel)
If a staging frontend deployment introduces severe visual regressions or routing breaks:
1. Open **Vercel Dashboard → `virtual-classroom-staging` → Deployments**.
2. Locate the previous healthy deployment.
3. Click the **three dots (...) → Instant Rollback** (or **Promote to Staging**).
4. Verify traffic immediately routes to the previous build.

### 6.2 Instant Backend Rollback (Render)
If a staging backend deployment fails health checks or exhibits runtime errors:
1. Open **Render Dashboard → `virtual-classroom-backend-staging` → Events**.
2. Locate the previous successful deploy event.
3. Click **Rollback to this deploy**.
4. Render will immediately redeploy the prior verified container image.

### 6.3 Emergency Administrator Account Revocation
If the bootstrapped administrator credentials are lost or compromised:
1. Open Supabase Dashboard → Staging Project (`zrbpjcfssbtxxgrjtsca`) → SQL Editor.
2. Execute a targeted revocation query:
   ```sql
   DELETE FROM public.refresh_tokens WHERE user_id = (SELECT id FROM public.users WHERE email = '<bootstrapped-email>');
   DELETE FROM public.users WHERE email = '<bootstrapped-email>' AND role = 'ROLE_ADMIN';
   ```
3. Re-execute the Bootstrap procedure in Section 3 with a new strong password.
