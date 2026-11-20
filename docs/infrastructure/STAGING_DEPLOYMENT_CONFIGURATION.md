# STAGING DEPLOYMENT CONFIGURATION & OPERATOR RUNBOOK
## Virtual Classroom Platform — Exact External Dashboard Setup, Admin Bootstrap & Rollback Procedures

**Last Updated:** 2026-11-18  
**Staging Integration Branch:** `develop`  
**Production Branch (STRICTLY ISOLATED):** `main`  
**Supabase Staging Project:** `virtual-classroom-staging` (`zrbpjcfssbtxxgrjtsca`, region `ap-south-1`, PostgreSQL `17.11`)

> **SECURITY RULE:** Never paste, commit, or transmit actual secret values in Git, chat, logs, or screenshots. Configure every secret directly in the target cloud provider's encrypted secret field.

---

## 1. Authoritative Staging Environment Architecture

| Layer | Target Service | Source Branch | Expected Endpoint / Identifier | Current Provisioning State |
| :--- | :--- | :---: | :--- | :---: |
| **Database** | Supabase (`virtual-classroom-staging`) | N/A (managed) | `aws-0-ap-south-1.pooler.supabase.com:5432/postgres` (`zrbpjcfssbtxxgrjtsca`) | **VERIFIED LIVE (PASS)** |
| **Object Storage** | Supabase Storage (private bucket) | N/A (managed) | `academic-assets` (`public = false`, 50 MB limit, MIME allowlist) | **VERIFIED PRIVATE (PASS)** |
| **GitHub Protection** | GitHub Environment `staging` | `develop` only | Reviewer `GreeshmanthSaiY`, `can_admins_bypass: false` | **VERIFIED LIVE (PASS)** |
| **GitHub Secrets** | GitHub Environment `staging` | `develop` | `5` required secrets (`RENDER_STAGING_DEPLOY_HOOK_URL`, `VERCEL_*`, `STAGING_BACKEND_URL`) | **MISSING (`0/5` configured)** |
| **Backend Hosting** | Render Web Service (`virtual-classroom-backend-staging`) | `develop` (`autoDeploy: false`) | `https://virtual-classroom-backend-staging.onrender.com` *(confirm exact URL upon creation)* | **NOT YET PROVISIONED (HTTP 404)** |
| **Frontend Hosting** | Vercel Staging Project (`virtual-classroom-staging`) | `develop` (Preview/Staging) | `https://virtual-classroom-staging.vercel.app` *(confirm exact URL upon creation)* | **NOT YET PROVISIONED (HTTP 404 `DEPLOYMENT_NOT_FOUND`)** |

---

## 2. Step-by-Step External Dashboard Setup Required from Operator

### Step 1 — Provision Render Staging Backend Service (`virtual-classroom-backend-staging`)
1. Open **Render Dashboard -> Blueprints** (or **New -> Web Service**) connected to `GreeshmanthSaiY/Virtual-Classroom-Platform`.
2. Create **ONLY** the staging service (`virtual-classroom-backend-staging`) from branch **`develop`** (do **NOT** create or touch `virtual-classroom-backend-prod` or `main`).
3. Verify exact service settings match `render.yaml`:
   - **Runtime:** `Docker`
   - **Dockerfile Path:** `./backend/Dockerfile`
   - **Docker Context:** `./backend`
   - **Branch:** `develop`
   - **Auto-Deploy:** **`No` (`autoDeploy: false`)**
   - **Health Check Path:** `/actuator/health/liveness`
4. Configure these exact **Environment Variables** in **Render Dashboard -> `virtual-classroom-backend-staging` -> Environment**:

| Variable Key | Value / Source | Secret? |
| :--- | :--- | :---: |
| `SPRING_PROFILES_ACTIVE` | `staging` | No |
| `DATABASE_URL` | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require` | Secret |
| `DATABASE_USERNAME` | `vc_app_user.zrbpjcfssbtxxgrjtsca` *(NEVER `postgres`)* | No |
| `DATABASE_PASSWORD` | Dedicated `vc_app_user` password from local gitignored `.env` (`APP_USER_DATABASE_PASSWORD`) | **Secret** |
| `JWT_SECRET` | Newly generated Base64 >= 256-bit staging signing secret (`openssl rand -base64 48`) | **Secret** |
| `JWT_EXPIRATION_MS` | `86400000` | No |
| `COOKIE_SECURE` | `true` | No |
| `COOKIE_SAME_SITE` | `None` | No |
| `CORS_ALLOWED_ORIGINS` | Exact Vercel staging HTTPS URL (e.g., `https://virtual-classroom-staging.vercel.app`, no trailing slash, no `localhost`) | No |
| `STORAGE_PROVIDER` | `supabase` | No |
| `SUPABASE_URL` | `https://zrbpjcfssbtxxgrjtsca.supabase.co` | No |
| `SUPABASE_SERVICE_ROLE_KEY` | Staging Supabase project (`zrbpjcfssbtxxgrjtsca`) **Settings -> API -> `service_role`** key *(NEVER production key)* | **Secret** |
| `SUPABASE_STORAGE_BUCKET` | `academic-assets` | No |
| `STAGING_ADMIN_BOOTSTRAP_ENABLED` | `false` *(keep `false` except during one-time bootstrap in Section 4)* | No |

5. Copy the **Deploy Hook URL** from **Render Dashboard -> `virtual-classroom-backend-staging` -> Settings -> Deploy Hook** (needed for Step 3).
6. Record the exact Render service HTTPS URL (e.g., `https://virtual-classroom-backend-staging.onrender.com`).

---

### Step 2 — Provision Vercel Staging Frontend Project (`virtual-classroom-staging`)
1. Open **Vercel Dashboard -> Add New -> Project** -> Import `GreeshmanthSaiY/Virtual-Classroom-Platform` as a **dedicated staging project** (e.g., `virtual-classroom-staging`; do **NOT** reuse a production Vercel project).
2. Configure **Project Settings -> General -> Build & Development Settings**:
   - **Framework Preset:** `Vite`
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
   - **Node.js Version:** `20.x`
3. Configure **Project Settings -> Git**:
   - Set Staging / Preview branch mapping to **`develop`** (and disable unintended production auto-deployments from `develop`).
4. Configure **Project Settings -> Environment Variables** (Preview / Staging scope ONLY):

| Variable Key | Value | Public / Secret |
| :--- | :--- | :---: |
| `VITE_API_BASE_URL` | `<RENDER_STAGING_BACKEND_URL>/api/v1` (e.g., `https://virtual-classroom-backend-staging.onrender.com/api/v1`) | Public *(browser-safe)* |

> **NEVER add to Vercel:** `SUPABASE_SERVICE_ROLE_KEY`, `DATABASE_PASSWORD`, `JWT_SECRET`, or `RENDER_STAGING_DEPLOY_HOOK_URL`.

5. Record from Vercel Dashboard:
   - **Vercel Project ID** (`Project Settings -> General -> Project ID`)
   - **Vercel Organization / Team ID** (`Account/Team Settings -> General -> Team ID`)
   - **Vercel Access Token** (`Account Settings -> Tokens -> Create Token`, scoped minimally to the staging team/project)
6. Ensure the final staging Vercel HTTPS origin matches `CORS_ALLOWED_ORIGINS` on Render **character-for-character** (no trailing `/`).

---

### Step 3 — Populate GitHub Environment `staging` Secrets
Navigate to:  
`GitHub Repository -> Settings -> Environments -> staging -> Environment secrets -> Add environment secret`

Add the following **5 required secrets** (and optional fallback):

| Exact Secret Name | Value Source |
| :--- | :--- |
| `RENDER_STAGING_DEPLOY_HOOK_URL` | Render Staging Service -> Settings -> Deploy Hook URL |
| `VERCEL_TOKEN` | Vercel Account Settings -> Tokens (scoped token) |
| `VERCEL_ORG_ID` | Vercel Team / Organization ID |
| `VERCEL_PROJECT_ID` | Vercel Staging Project ID |
| `STAGING_BACKEND_URL` | Exact Render staging backend base URL (e.g., `https://virtual-classroom-backend-staging.onrender.com`) |
| `STAGING_FRONTEND_URL` *(optional fallback)* | Exact Vercel staging frontend base URL (e.g., `https://virtual-classroom-staging.vercel.app`) |

*(Do **NOT** modify protection rules: keep `Required reviewers: GreeshmanthSaiY`, `can_admins_bypass: false`, and `Selected branches: develop`.)*

---

## 3. Authorized Staging Deployment Execution Sequence

Once Steps 1–3 are completed:
1. **Dry-Run First (Mandatory Safe Verification):**
   - Go to **GitHub Actions -> `Staging Deployment` -> Run workflow**.
   - Branch: **`develop`**
   - `dry_run_only`: **`true`**
   - `confirm_live_deployment`: **`false`**
   - Approve the `staging` environment review gate -> confirm dry-run succeeds with zero deployments.
2. **Confirmed Live Staging Deployment:**
   - Go to **GitHub Actions -> `Staging Deployment` -> Run workflow**.
   - Branch: **`develop`**
   - `dry_run_only`: **`false`**
   - `confirm_live_deployment`: **`true`**
   - `skip_smoke_tests`: **`false`**
   - Approve the `staging` environment review gate when prompted.
   - The workflow will trigger Render staging, build & deploy Vercel staging, poll `/actuator/health/liveness` up to 8 minutes for rollout readiness, and run the 9-check `scripts/staging_smoke_tests.js` suite.

---

## 4. Secure One-Time Staging Admin Bootstrap (Phase H)

Implemented in `backend/src/main/java/com/virtualclassroom/bootstrap/StagingAdminBootstrapRunner.java`:
- **Profile Isolation:** Annotated strictly with `@Profile("staging")` AND runtime-guarded to throw `IllegalStateException` if `prod`/`production` is active or `staging` is absent.
- **Default Off:** Disabled unless `STAGING_ADMIN_BOOTSTRAP_ENABLED=true`.
- **Credential Hygiene:** Zero hardcoded credentials, zero default passwords, rejects prohibited demo emails (`admin@virtualclassroom.edu`, etc.), requires password length `>= 14` with uppercase + lowercase + digit + special character, BCrypt-hashed, never logged.
- **Idempotency:** No-op if any `ROLE_ADMIN` user or the target email already exists.

### Operator Bootstrap Procedure (Run Once After Render Staging Is Live)
1. In **Render Dashboard -> `virtual-classroom-backend-staging` -> Environment**, temporarily set:
   - `STAGING_ADMIN_BOOTSTRAP_ENABLED=true`
   - `STAGING_ADMIN_EMAIL=<operator-staging-admin-email>`
   - `STAGING_ADMIN_FULL_NAME=Staging Administrator`
   - `STAGING_ADMIN_PASSWORD=<strong-unique-14+-char-password>`
2. Trigger a single restart/redeploy on Render and verify in logs:
   `Staging administrator bootstrap completed for <email>.` (confirm zero password characters appear in logs).
3. **Immediately** remove `STAGING_ADMIN_PASSWORD` and set `STAGING_ADMIN_BOOTSTRAP_ENABLED=false` in Render Dashboard.

### Safe Rollback for Admin Bootstrap
- Disable `STAGING_ADMIN_BOOTSTRAP_ENABLED=false` and unset `STAGING_ADMIN_PASSWORD`.
- To revoke/remove the bootstrapped admin account, suspend the user via Admin API or execute a targeted non-destructive SQL rollback in Supabase Staging SQL Editor:
  ```sql
  DELETE FROM public.refresh_tokens WHERE user_id = (SELECT id FROM public.users WHERE email = '<bootstrapped-email>');
  DELETE FROM public.users WHERE email = '<bootstrapped-email>' AND role = 'ROLE_ADMIN';
  ```

---

## 5. Password Reset & Email Delivery Configuration Status (Phase I)

Verified directly in `backend/src/main/java/com/virtualclassroom/service/AuthService.java`, `AuthController.java`, and `backend/pom.xml`:
- **Implemented & Verified:**
  - Endpoints `POST /api/v1/auth/forgot-password` and `POST /api/v1/auth/reset-password` exist and prevent user enumeration.
  - Tokens use 256-bit `SecureRandom`, stored exclusively as SHA-256 hashes (`password_reset_tokens.token_hash`), expire after `15 minutes`, invalidate prior active tokens, enforce single-use replay protection (`used = true`), and revoke all active refresh tokens upon reset.
- **NOT Implemented (Honest Gap):**
  - `backend/pom.xml` has **no** `spring-boot-starter-mail` or third-party email SDK.
  - `AuthService.forgotPassword` generates and persists the hashed token, but **does not dispatch any email**.
  - **Consequence:** Self-service email password reset is **NOT operational in staging** until an SMTP/transactional email provider (e.g., Resend, Postmark, SendGrid, or AWS SES via `spring-boot-starter-mail` with server-side `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` env vars) is integrated in a dedicated feature milestone. Authenticated `POST /api/v1/auth/change-password` IS fully operational without SMTP.

---

## 6. Staging Rollback Procedures

1. **Backend Rollback (Render):** Open **Render Dashboard -> `virtual-classroom-backend-staging` -> Events -> Rollback** on the last known-good deploy (or revert the commit on `develop` via PR and run manual `Staging Deployment`).
2. **Frontend Rollback (Vercel):** Open **Vercel Dashboard -> `virtual-classroom-staging` -> Deployments -> Instant Rollback** on the previous healthy preview/staging deployment.
3. **Database Rollback:** Schema migrations V1–V5 are already verified and frozen (`spring.flyway.enabled=false` at runtime); role/ACL rollback script is versioned at `docs/infrastructure/sql/M18_3_ROLLBACK.sql` (never execute without explicit authorization).
