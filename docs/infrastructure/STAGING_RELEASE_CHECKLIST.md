# STAGING RELEASE CHECKLIST & GATEWAY MATRIX
## Virtual Classroom Platform — Phase-by-Phase Release Execution & Verification Gates

**Target Environment:** Staging  
**Target Integration Branch:** `develop`  
**Deploy Orchestrator:** GitHub Actions (`.github/workflows/staging-deployment.yml`)  
**Production Isolation:** Production (`main`) is strictly isolated; zero changes to production are permitted.

---

## Stage 0: Pre-Deployment Readiness & Code Quality Gates

| # | Check / Requirement | Target / Tool | Expected Result | Status |
| :-: | :--- | :--- | :--- | :---: |
| 0.1 | Branch Integrity | `origin/develop` | PR #6 and PR #7 merged; 0 unreviewed commits on `main`. | **READY** |
| 0.2 | Workflow Defect Correction | `.github/workflows/staging-deployment.yml` | Line 292 references `steps.validate-urls.outputs.backend_url`. | **READY** |
| 0.3 | Backend Test Suite | Maven (`./mvnw test`) | 147 tests run, 141 passed, 0 failed, 6 Windows-pg skips. | **READY** |
| 0.4 | Frontend Build & Vitest | Vite / Vitest | TypeScript build clean (`1887` modules), 37/37 tests passed. | **READY** |
| 0.5 | Frontend Bundle Security Scan | Dist Asset Inspection | 0 `service_role` keys, 0 hardcoded localhost URLs, 0 secrets. | **READY** |
| 0.6 | Smoke Test Specification | `node scripts/staging_smoke_tests.js --dry-run` | All 9 smoke test specifications pass in dry-run mode. | **READY** |
| 0.7 | YAML Integrity | Repository YAML Linter | 12/12 YAML files valid (workflows, render, profiles, compose). | **READY** |

---

## Stage 1: External Cloud Provisioning (Operator Dashboard Actions)

*Execute these steps in your Render and Vercel dashboards before attempting any staging deployment.*

- [ ] **1.1 Provision Render Staging Web Service (`virtual-classroom-backend-staging`):**
  - Repository: `GreeshmanthSaiY/Virtual-Classroom-Platform`
  - Branch: **`develop`**
  - Runtime: `Docker` (Dockerfile: `./backend/Dockerfile`, Context: `./backend`)
  - Auto-Deploy: **`No` (`autoDeploy: false`)**
  - Health Check Path: `/actuator/health/liveness`
- [ ] **1.2 Provision Vercel Staging Frontend Project (`virtual-classroom-staging`):**
  - Repository: `GreeshmanthSaiY/Virtual-Classroom-Platform`
  - Staging / Preview Branch: **`develop`**
  - Framework Preset: `Vite`
  - Root Directory: `frontend`
  - Build Command: `npm run build`
  - Output Directory: `dist`
  - Node.js Version: `20.x`

---

## Stage 2: Secret & Environment Configuration (Operator Setup)

### 2.1 GitHub Environment `staging` Secrets
Navigate to: **GitHub Repo → Settings → Environments → staging → Environment secrets**
- [ ] `RENDER_STAGING_DEPLOY_HOOK_URL`: Copied from Render service settings.
- [ ] `VERCEL_TOKEN`: Scoped Vercel personal/team access token.
- [ ] `VERCEL_ORG_ID`: Vercel team/account identifier.
- [ ] `VERCEL_PROJECT_ID`: Vercel `virtual-classroom-staging` project identifier.
- [ ] `STAGING_BACKEND_URL`: Base HTTPS URL of Render staging backend (e.g. `https://virtual-classroom-backend-staging.onrender.com`).
- [ ] *(Optional)* `STAGING_FRONTEND_URL`: Base HTTPS URL of Vercel staging frontend (e.g. `https://virtual-classroom-staging.vercel.app`).

### 2.2 Render Staging Backend Environment Variables
Configure in: **Render Dashboard → `virtual-classroom-backend-staging` → Environment**
- [ ] `SPRING_PROFILES_ACTIVE` = `staging`
- [ ] `PORT` = `8080`
- [ ] `DATABASE_URL` = `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
- [ ] `DATABASE_USERNAME` = `vc_app_user.zrbpjcfssbtxxgrjtsca`
- [ ] `DATABASE_PASSWORD` = *(Dedicated `vc_app_user` password from local `.env`)*
- [ ] `JWT_SECRET` = *(Cryptographically strong Base64 >= 256-bit string)*
- [ ] `JWT_EXPIRATION_MS` = `86400000`
- [ ] `COOKIE_SECURE` = `true`
- [ ] `COOKIE_SAME_SITE` = `None`
- [ ] `CORS_ALLOWED_ORIGINS` = `https://virtual-classroom-staging.vercel.app` *(Exact Vercel URL, no trailing `/`)*
- [ ] `STORAGE_PROVIDER` = `supabase`
- [ ] `SUPABASE_URL` = `https://zrbpjcfssbtxxgrjtsca.supabase.co`
- [ ] `SUPABASE_SERVICE_ROLE_KEY` = *(Staging project `service_role` secret from Supabase Dashboard)*
- [ ] `SUPABASE_STORAGE_BUCKET` = `academic-assets`
- [ ] `STAGING_ADMIN_BOOTSTRAP_ENABLED` = `false` *(Keep false until Stage 7)*

### 2.3 Vercel Staging Frontend Environment Variables
Configure in: **Vercel Project Settings → Environment Variables** (Preview / Staging scope):
- [ ] `VITE_API_BASE_URL` = `<RENDER_STAGING_BACKEND_URL>/api/v1`

---

## Stage 3: Pre-Deployment Dry-Run Verification

- [ ] **3.1 Trigger Manual Dry-Run in GitHub Actions:**
  - Workflow: **`Staging Deployment`**
  - Branch: **`develop`**
  - Inputs: `dry_run_only: true`, `confirm_live_deployment: false`
- [ ] **3.2 Approve Reviewer Gate:**
  - Operator (`GreeshmanthSaiY`) approves GitHub Environment `staging` gate.
- [ ] **3.3 Verify Preflight Output:**
  - Secrets validation succeeds (live deploy skipped).
  - Dry-run smoke test specification passes (9/9).
  - Zero deployment hooks dispatched.

---

## Stage 4: Live Staging Deployment Execution

- [ ] **4.1 Trigger Confirmed Live Deployment:**
  - Workflow: **`Staging Deployment`**
  - Branch: **`develop`**
  - Inputs: `dry_run_only: false`, `confirm_live_deployment: true`, `skip_smoke_tests: false`
- [ ] **4.2 Approve Reviewer Gate:**
  - Operator approves the live deployment prompt.
- [ ] **4.3 Monitor Deployment Steps:**
  - Render hook triggered (HTTP 200/201).
  - Vercel build & prebuilt deploy finishes with preview URL.
  - Rollout readiness poll waits up to 8 minutes for `/actuator/health/liveness` HTTP 200 UP.
  - Automated smoke tests execute against target endpoints (9/9 passed).

---

## Stage 5: Initial Administrator Bootstrap (Phase H)

- [ ] **5.1 Configure Bootstrap Environment Variables in Render:**
  - Set `STAGING_ADMIN_BOOTSTRAP_ENABLED` = `true`
  - Set `STAGING_ADMIN_EMAIL` = `<operator-admin-email>`
  - Set `STAGING_ADMIN_PASSWORD` = `<strong-unique-14+-char-password>`
  - Set `STAGING_ADMIN_FULL_NAME` = `Staging Administrator`
- [ ] **5.2 Restart Backend Container:**
  - Trigger manual redeploy in Render.
- [ ] **5.3 Verify Bootstrap in Logs:**
  - Confirm message: `Staging administrator bootstrap completed for <email>.`
  - Confirm zero password characters appear in Render logs.
- [ ] **5.4 Immediately De-configure Bootstrap:**
  - Set `STAGING_ADMIN_BOOTSTRAP_ENABLED` = `false`.
  - Clear `STAGING_ADMIN_PASSWORD`.
  - Save changes in Render Dashboard.

---

## Stage 6: Post-Deployment Smoke & Verification Checklist

- [ ] **6.1 Public Route Access:**
  - Landing page loads without console errors (`GET /`).
  - Login page loads (`GET /login`).
- [ ] **6.2 Authentication:**
  - Admin login succeeds with bootstrapped credentials.
  - JWT token stored in-memory; refresh token cookie set with `HttpOnly; Secure; SameSite=None`.
- [ ] **6.3 Admin Dashboard:**
  - Admin metrics load (`GET /api/v1/admin/metrics`).
  - Academic course/subject list renders correctly.
- [ ] **6.4 Student & Faculty Flows:**
  - Student registration request submitted (status: `PENDING`).
  - Admin approves registration request.
  - Student logs in and browses available courses.
- [ ] **6.5 Private File Storage:**
  - Faculty/Admin uploads course asset.
  - Signed download URL generated (900s expiry).
  - Unauthenticated access to private bucket directly returns HTTP 400 (`NoSuchBucket`).
  - Disabled `/api/v1/storage/files/*` route returns HTTP 401/403.
- [ ] **6.6 CORS Enforcement:**
  - Requests from unauthorized third-party origins are rejected by CORS headers.

---

## Stage 7: Rollback Verification Readiness

- [ ] **Vercel Instant Rollback:** Confirmed available in Vercel Dashboard → Deployments → Instant Rollback.
- [ ] **Render Rollback:** Confirmed available in Render Dashboard → Events → Rollback.
- [ ] **Database ACL Rollback Script:** Preserved at `docs/infrastructure/sql/M18_3_ROLLBACK.sql`.
