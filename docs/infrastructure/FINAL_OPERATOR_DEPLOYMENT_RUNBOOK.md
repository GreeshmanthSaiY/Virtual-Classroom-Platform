# FINAL OPERATOR DEPLOYMENT RUNBOOK
**Virtual Classroom Platform — Step-by-Step Staging Launch Guide**  
**Date:** 2026-11-20  
**Target:** Operator / Release Engineer  
**Audience:** Beginner-Friendly & Production-Safe Execution  

---

## Important Safety Rules Before You Begin

> [!CAUTION]
> - **NEVER** commit secrets, database passwords, or API keys into git.
> - **NEVER** share or paste database passwords or `service_role` keys into chat assistants or public tickets.
> - **NEVER** modify or deploy to the production Supabase database (`tzbjmxtmysvzxdmspiui`).
> - Staging uses Supabase project reference: **`zrbpjcfssbtxxgrjtsca`**.

---

## Step 1: Review and Merge Documentation PR #9 (GitHub)

1. Open your browser and navigate to:  
   👉 **[Pull Request #9](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/pull/9)**
2. Verify the title: `docs(release): final release readiness, pre-deployment checklist, and verification runbooks`.
3. Verify that only documentation and milestone files under `docs/` are modified (15 files).
4. Click **Approve** and click **Merge pull request** (use **Create a merge commit** or **Squash and merge**).
5. The `develop` branch is now 100% updated with all release assets.

---

## Step 2: Verify GitHub Staging Environment Settings (GitHub)

1. In your GitHub repository, navigate to:  
   **Settings** (top tab) -> **Environments** (left sidebar) -> Click **`staging`**.
2. Verify **Deployment branches and tags**:
   - Ensure it is set to **Selected branches**.
   - Ensure the allowed branch rule is strictly: **`develop`**.
3. Verify **Required reviewers**:
   - Ensure **Required reviewers** is checked.
   - Ensure your GitHub username (`GreeshmanthSaiY`) is added as the reviewer.
   - Ensure **Prevent self-review** or **Prevent admin bypass** is enforced.
4. Keep this page open; you will add secrets here in Step 5.

---

## Step 3: Create and Configure Staging Backend (Render)

1. Log in to your **[Render Dashboard](https://dashboard.render.com)**.
2. Click **New +** (top right) -> Select **Web Service**.
3. Connect your repository: `GreeshmanthSaiY/Virtual-Classroom-Platform`.
4. Configure the basic service settings:
   - **Name:** `virtual-classroom-backend-staging`
   - **Region:** `Oregon (US West)` (or your preferred region)
   - **Branch:** `develop`
   - **Root Directory:** *(leave blank)*
   - **Runtime:** `Docker`
   - **Dockerfile Path:** `./backend/Dockerfile`
   - **Docker Build Context:** `./backend`
   - **Instance Type:** `Starter` (or `Free` for testing)
5. Scroll down to **Health Check Path**:
   - Enter: `/actuator/health/liveness`
6. Click **Advanced** and configure:
   - **Auto-Deploy:** Toggle to **No** (this ensures only GitHub Actions can trigger deployment).
7. Scroll down to **Environment Variables** and click **Add Environment Variable** for each:

| Key | Value / Source |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `staging` |
| `PORT` | `8080` |
| `DATABASE_URL` | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` |
| `DATABASE_USERNAME` | `vc_app_user.zrbpjcfssbtxxgrjtsca` |
| `DATABASE_PASSWORD` | *(Your secure password for vc_app_user)* |
| `JWT_SECRET` | *(Your 256-bit secure secret key, at least 32 characters)* |
| `JWT_EXPIRATION_MS` | `86400000` |
| `COOKIE_SECURE` | `true` |
| `COOKIE_SAME_SITE` | `None` |
| `CORS_ALLOWED_ORIGINS` | `https://virtual-classroom-staging.vercel.app` |
| `STORAGE_PROVIDER` | `supabase` |
| `SUPABASE_URL` | `https://zrbpjcfssbtxxgrjtsca.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | *(Your staging Supabase service_role secret key)* |
| `SUPABASE_STORAGE_BUCKET` | `academic-assets` |
| `STAGING_ADMIN_BOOTSTRAP_ENABLED` | `true` |
| `STAGING_ADMIN_EMAIL` | `staging-admin@virtualclassroom.internal` |
| `STAGING_ADMIN_PASSWORD` | *(Your strong initial staging admin password)* |
| `STAGING_ADMIN_FULL_NAME` | `Staging Administrator` |

8. Click **Create Web Service**.
9. Once the service page loads, go to **Settings** (left sidebar) -> Scroll down to **Deploy Hook**.
10. Click **Create Deploy Hook**.
11. Copy the webhook URL (it looks like `https://api.render.com/deploy/srv-...`). Save it securely for Step 5.

---

## Step 4: Create and Configure Staging Frontend (Vercel)

1. Log in to your **[Vercel Dashboard](https://vercel.com/dashboard)**.
2. Click **Add New...** -> Select **Project**.
3. Import the repository: `GreeshmanthSaiY/Virtual-Classroom-Platform`.
4. Configure Project settings:
   - **Project Name:** `virtual-classroom-staging`
   - **Framework Preset:** `Vite`
   - **Root Directory:** Click **Edit** and select `frontend` (Click **Continue**).
5. Expand **Environment Variables** and add:
   - **Key:** `VITE_API_BASE_URL`
   - **Value:** `https://virtual-classroom-backend-staging.onrender.com/api/v1`  
     *(Replace with your actual Render backend URL if different)*
6. Click **Deploy** to establish initial deployment.
7. Once deployed, go to **Settings** (top tab):
   - Under **General**, find **Project ID** (e.g. `prj_...`). Copy and save it.
8. Go to your **[Vercel Account Settings -> Tokens](https://vercel.com/account/tokens)**:
   - Click **Create Token**.
   - Name: `github-actions-staging`.
   - Scope: Full account or selected team.
   - Copy the generated token. Save it for Step 5.
9. Note your **Vercel Org ID / Team ID**:
   - If using a personal account, your Org ID is in your personal settings or team URL.

---

## Step 5: Add GitHub Staging Environment Secrets (GitHub)

1. In your GitHub repository, navigate to:  
   **Settings** -> **Environments** -> Click **`staging`**.
2. Scroll to **Environment secrets** -> Click **Add secret**.
3. Add the following 6 secrets:

| Secret Name | Value to Paste |
|---|---|
| `RENDER_STAGING_DEPLOY_HOOK_URL` | The Render Deploy Hook URL copied in Step 3 (starts with `https://api.render.com/deploy/...`). |
| `STAGING_BACKEND_URL` | Base Render URL: `https://virtual-classroom-backend-staging.onrender.com` |
| `STAGING_FRONTEND_URL` | Base Vercel URL: `https://virtual-classroom-staging.vercel.app` |
| `VERCEL_TOKEN` | The Personal Access Token generated in Step 4. |
| `VERCEL_ORG_ID` | Your Vercel Team ID or User ID from Step 4. |
| `VERCEL_PROJECT_ID` | The Vercel Project ID (`prj_...`) copied in Step 4. |

---

## Step 6: Trigger Gated Staging Deployment (GitHub Actions)

1. In your GitHub repository, navigate to the **Actions** tab.
2. In the left workflow list, click **Staging Deployment**.
3. On the right, click **Run workflow**:
   - **Use workflow from:** `develop`
   - **Run pre-deployment verification in DRY-RUN mode...:** **Uncheck (set to false)**.
   - **Explicit operator confirmation required for LIVE staging deployment...:** **Check (set to true)**.
   - **Skip post-deployment automated smoke tests...:** Leave **Unchecked (false)**.
4. Click the green **Run workflow** button.
5. In the run list, click on the active run.
6. When the workflow reaches the deployment gate, a yellow notification will state:  
   *"Waiting for review: staging"*.
7. Click **Review deployments** -> Select **staging** -> Click **Approve and deploy**.
8. Watch the pipeline execute:
   - Preflight verification and secret validation pass.
   - Render webhook triggered.
   - Vercel frontend deployed.
   - Rollout polling monitors Render `/actuator/health/liveness` until HTTP 200.
   - Automated live smoke tests execute and pass all 9 checks.

---

## Step 7: Post-Deployment Smoke Verification

### 7.1 Automated Suite Verification
You can also run the automated smoke test script locally against the live deployment:
```bash
node scripts/staging_smoke_tests.js \
  --frontend=https://virtual-classroom-staging.vercel.app \
  --backend=https://virtual-classroom-backend-staging.onrender.com
```

### 7.2 Manual Acceptance Checks
1. **Frontend Landing:** Open `https://virtual-classroom-staging.vercel.app` in your browser. Verify the page renders cleanly with no console errors.
2. **SPA Routing:** Direct navigate to `/login`. Verify the login screen renders without a 404 error.
3. **Admin Login:** Log in using the bootstrapped administrator credentials:
   - Email: `staging-admin@virtualclassroom.internal`
   - Password: *(The password you configured in Step 3)*
4. **Backend Liveness:** In your terminal, run:
   ```bash
   curl -i https://virtual-classroom-backend-staging.onrender.com/actuator/health
   ```
   Verify response is `HTTP 200 OK` with `{"status":"UP"}`.

---

## Step 8: Emergency Rollback Procedure

If any critical issue occurs post-deployment:

### Render Rollback:
1. In Render Dashboard -> Web Service -> **Deploys**.
2. Find the previous stable build and click the three dots (`...`) -> **Rollback to this deploy**.

### Vercel Rollback:
1. In Vercel Dashboard -> Project -> **Deployments**.
2. Find the previous deployment and click the three dots (`...`) -> **Promote to Production**.

### Database:
- No database rollback is required for normal releases since migrations V1–V5 are backward-compatible.
- In case of critical emergency data recovery, execute `docs/infrastructure/sql/M18_3_ROLLBACK.sql` in the Supabase staging SQL editor.
