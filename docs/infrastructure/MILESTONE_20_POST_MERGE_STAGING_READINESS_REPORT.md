# MILESTONE 20 — POST-MERGE STAGING READINESS AUDIT REPORT
## Virtual Classroom Platform — Comprehensive Read-Only Verification of Remote `develop`

**Audit Date:** 2026-11-20  
**Repository:** `GreeshmanthSaiY/Virtual-Classroom-Platform`  
**Target Integration Branch:** `develop`  
**Production Branch:** `main`  
**Execution Mode:** **STRICTLY READ-ONLY** — Zero application/workflow edits, zero commits, zero pushes, zero PRs created/merged, zero workflow triggers, zero cloud deployments, zero database/SQL connections, zero secret value disclosures.

---

## 1. Branch SHA & Merge Verification

Verified directly via Git inspection against freshly fetched remote tips (`git fetch origin develop main`) and live GitHub REST API:

| Ref / Target | Verified Exact Commit SHA | Short SHA | Commit Subject / Description |
| :--- | :--- | :---: | :--- |
| **Remote `develop` (`origin/develop`)** | `c197d262afa647c2da500025031a421f082b20eb` | `c197d26` | `Merge pull request #7 from GreeshmanthSaiY/feature/staging-bootstrap-and-smoke-hardening` |
| **PR #7 Merge Commit** | `c197d262afa647c2da500025031a421f082b20eb` | `c197d26` | Merged at `2026-11-20T15:23:52Z` (state: `closed`, merged: `true`) |
| **PR #7 Head Commit** | `64bd6aebfab5810237ca5d54b4231b539c3666b6` | `64bd6ae` | `feat(staging): add secure staging admin bootstrap, rollout health poll, strict smoke checks & M19.4 reports` |
| **PR #6 Merge Commit** | `036f0381ee459d807ab5857d13de17a74a8cfdfd` | `036f038` | Merged at `2026-11-18T16:38:09Z` (state: `closed`, merged: `true`) |
| **Remote `main` (`origin/main`)** | `3632edddd1d5784c41ec9791520afc5c41400611` | `3632edd` | `Merge pull request #3 from GreeshmanthSaiY/ci/bootstrap-workflows` *(Production strictly untouched)* |
| **Divergence (`origin/main...origin/develop`)** | Behind: `0`, Ahead: `53` | N/A | `develop` is 53 commits ahead of `main`; production branch is completely isolated and unmodified. |

**Verification Status:** **PASS** — Both PR #6 (`036f038`) and PR #7 (`c197d26`) merge commits are present in the authoritative commit ancestry of `origin/develop`.

---

## 2. Comprehensive Required Checks & Results Matrix

| # | Check Dimension | Status | Verified Evidence on `origin/develop` |
| :-: | :--- | :---: | :--- |
| **1** | **Latest Remote `develop` SHA** | **PASS** | Exact SHA recorded: `c197d262afa647c2da500025031a421f082b20eb`. |
| **2** | **PR #6 and PR #7 Merge Verification** | **PASS** | Both merge commits confirmed present in `git log --merges origin/develop`. |
| **3.1** | **Staging Workflow: Manual Trigger Only** | **PASS** | [.github/workflows/staging-deployment.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L17-L35): `on:` contains strictly `workflow_dispatch:`; `push:` trigger is completely absent. |
| **3.2** | **Staging Workflow: Safe-by-Default Inputs** | **PASS** | [.github/workflows/staging-deployment.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L20-L29): `inputs.dry_run_only.default: true`, `inputs.confirm_live_deployment.default: false`. |
| **3.3** | **Staging Workflow: Dual Explicit Confirmation** | **PASS** | [.github/workflows/staging-deployment.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L83-L93): Fails fast unless both `dry_run_only == false` AND `confirm_live_deployment == true`. |
| **3.4** | **Staging Workflow: Strict `develop` Branch Guard** | **PASS** | [.github/workflows/staging-deployment.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L57-L66): Preflight script enforces `refs/heads/develop` (`exit 1` on mismatch); jobs and deploy steps re-verify `github.ref == 'refs/heads/develop'`. |
| **3.5** | **Staging Workflow: GitHub Environment `staging`** | **PASS** | Attached to all 3 deployment/validation jobs: `staging-preflight` ([L51](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L51)), `deploy-staging` ([L153](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L153)), and `staging-smoke-tests` ([L251](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L251)). |
| **3.6** | **Staging Workflow: Deployment Step Guards** | **PASS** | Render deploy ([L172-L196](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L172-L196)) and Vercel deploy ([L201-L234](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L201-L234)) require `allow_live_deploy == 'true'`, `develop` ref, and non-empty secret checks. |
| **3.7** | **Staging Workflow: Gated Smoke Tests** | **PASS** | `staging-smoke-tests` ([L240-L249](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L240-L249)) requires `needs.staging-preflight.result == 'success'`, `needs.deploy-staging.result == 'success'`, `allow_live_deploy == 'true'`, `backend_status == 'DEPLOYED'`, and `skip_smoke_tests != true`. |
| **4.1** | **Render Config: Staging Runtime DB User** | **PASS** | [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L23-L24): `DATABASE_USERNAME: vc_app_user.zrbpjcfssbtxxgrjtsca` (tenant-qualified, unprivileged runtime role). |
| **4.2** | **Render Config: Supabase Staging Reference** | **PASS** | [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L36-L42): `STORAGE_PROVIDER: supabase`, `SUPABASE_STORAGE_BUCKET: academic-assets`, `SUPABASE_URL` externalized via secret. |
| **4.3** | **Render Config: Runtime Flyway Disabled** | **PASS** | [backend/src/main/resources/application-staging.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/src/main/resources/application-staging.yml#L36-L39): `spring.flyway.enabled: false`, `baseline-on-migrate: false`; `hibernate.ddl-auto: validate` preserved. |
| **4.4** | **Render Config: Auto-Deploy Disabled** | **PASS** | [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L15): `autoDeploy: false` on staging; [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L54): `autoDeploy: false` on prod. |
| **4.5** | **Render Config: Production Secrets Externalized** | **PASS** | [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L60-L80): `sync: false` for prod `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`. |
| **5.1** | **Backend Staging Profile & JWT Security** | **PASS** | [backend/src/main/resources/application-staging.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/src/main/resources/application-staging.yml#L6-L8): `server.error.include-message: never`, `include-binding-errors: never`; `JwtTokenProvider.java` rejects default development secret in `staging`. |
| **5.2** | **Backend Staging CORS Policy** | **PASS** | [backend/src/main/resources/application-staging.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/src/main/resources/application-staging.yml#L49) & [render.yaml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/render.yaml#L34): `CORS_ALLOWED_ORIGINS` strictly set to `https://virtual-classroom-staging.vercel.app` (`http://localhost:5173` removed). |
| **5.3** | **Backend Private Storage Signing** | **PASS** | `FileStorageService.java` generates signed URLs via Supabase Storage REST API with canonicalized path and MIME validation; `StorageAuthorizationService.java` enforces ownership; `/api/v1/storage/files/**` returns `403` when cloud storage is active. |
| **5.4** | **Backend Admin Bootstrap Implementation** | **PASS** | [StagingAdminBootstrapRunner.java](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/src/main/java/com/virtualclassroom/bootstrap/StagingAdminBootstrapRunner.java): `@Profile("staging")`-only, runtime fail-closed guard against `prod`, disabled by default, requires strong password (>= 14 chars, 4 classes), rejects demo emails, idempotent. |
| **6.1** | **Frontend Production Build Config & API Base URL** | **PASS** | [frontend/src/core/api/client.ts](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/frontend/src/core/api/client.ts#L4): `const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'`; `withCredentials: true` enables HttpOnly cookie transport; pure in-memory access token storage. |
| **6.2** | **Absence of Secrets in Frontend Bundle** | **PASS** | Scanned all `47` compiled assets in `frontend/dist`: `0` `service_role` keys, `0` hardcoded localhost URLs, `0` Supabase project keys, `0` backend secrets. |
| **7** | **Supabase Configuration Reference** | **PASS** | Staging project reference `zrbpjcfssbtxxgrjtsca`, region `ap-south-1`, private bucket `academic-assets` correctly specified across `render.yaml` and `application-staging.yml`. *(No database connection or SQL executed per strict read-only directive).* |
| **8** | **CI Checks & Recent Actions Results** | **PASS** | All recent workflow runs on PR #6, PR #7, and `develop` completed with **100% SUCCESS** (`44762870414`, `44354195557`, `44354195227`, `44349511111`, `44321518964`, `44321518786`). |
| **9** | **Cloud Secrets & Environment Inventory** | **FAIL (BLOCKED)** | GitHub Environment `staging` has `0` secrets configured (`0/5` required); Render staging service not yet provisioned (`404`); Vercel staging project not yet deployed (`404`). |

---

## 3. GitHub Actions CI History on Merged PRs & `develop`

Queried live via GitHub REST API:

| Run ID | Workflow | Event | Branch | Commit SHA | Conclusion | Significance |
| :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **`44762870414`** | Integration Validation | `push` | `develop` | `c197d262af` | **`success`** | Direct post-merge CI gate of PR #7 on `develop`. |
| **`44354195557`** | Integration Validation | `pull_request` | `feature/staging-bootstrap-...` | `64bd6aebfa` | **`success`** | Full-stack integration test for PR #7. |
| **`44354195227`** | Pull Request Validation | `pull_request` | `feature/staging-bootstrap-...` | `64bd6aebfa` | **`success`** | 4/4 jobs passed (Frontend QA, Backend QA with all 7 new bootstrap tests, PG17 gate, Docker build). |
| **`44349511111`** | Integration Validation | `push` | `develop` | `036f0381ee` | **`success`** | Direct post-merge CI gate of PR #6 on `develop`. |
| **`44321518964`** | Integration Validation | `pull_request` | `feature/staging-readiness-...` | `4edc86ab6c` | **`success`** | Full-stack integration test for PR #6. |
| **`44321518786`** | Pull Request Validation | `pull_request` | `feature/staging-readiness-...` | `4edc86ab6c` | **`success`** | 4/4 jobs passed for PR #6. |

---

## 4. Defect Detected in `.github/workflows/staging-deployment.yml` (Pre-Deployment Bug)

During detailed inspection of [.github/workflows/staging-deployment.yml](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.github/workflows/staging-deployment.yml#L290-L306) on `develop`, an output step reference mismatch was identified:

```yaml
# Step 1: Endpoint validation step declares id: validate-urls (lines 262-263)
- name: Validate target endpoints
  id: validate-urls
  ...
  run: |
    echo "backend_url=$BACKEND_URL" >> "$GITHUB_OUTPUT"

# Step 2: Rollout polling step references non-existent id: resolve-urls (line 292)
- name: Wait for Render Staging Backend Rollout Readiness
  env:
    BACKEND_URL: ${{ steps.resolve-urls.outputs.backend_url }}  # <--- DEFECT: Should be steps.validate-urls.outputs.backend_url
```

### Impact
If a live deployment workflow were triggered, Step 2 would evaluate `BACKEND_URL` as an empty string. The polling script would execute `curl "${BACKEND_CLEAN}/actuator/health/liveness"` without a host, resulting in a fatal script error and aborting the workflow before smoke tests could run.

### Resolution Required Before Live Deployment Run
In `.github/workflows/staging-deployment.yml` line 292, update:
`steps.resolve-urls.outputs.backend_url` → `steps.validate-urls.outputs.backend_url`.

---

## 5. Required Secret & Environment Variable Inventory (NAMES ONLY)

> **HYGIENE RULE:** Never print, request, or expose secret values. All credentials must be configured securely by the operator directly within the respective dashboard interfaces.

### 5.1 GitHub Environment `staging` Secrets (Currently `0/5` Configured — BLOCKED)
Navigate to: `GitHub Repo -> Settings -> Environments -> staging -> Environment secrets`

| Required Secret Name | Expected Source Provider | Purpose | Status |
| :--- | :--- | :--- | :---: |
| `RENDER_STAGING_DEPLOY_HOOK_URL` | Render Dashboard | Webhook to trigger backend staging deployment | **MISSING** |
| `VERCEL_TOKEN` | Vercel Account Settings | CLI authentication token for preview/staging deployment | **MISSING** |
| `VERCEL_ORG_ID` | Vercel Team Settings | Organization identifier | **MISSING** |
| `VERCEL_PROJECT_ID` | Vercel Project Settings | Dedicated staging project identifier | **MISSING** |
| `STAGING_BACKEND_URL` | Render Service Settings | Target base URL for backend health probes & smoke tests | **MISSING** |
| `STAGING_FRONTEND_URL` *(optional fallback)* | Vercel Project Settings | Fallback target frontend URL for smoke tests | **OPTIONAL** |

### 5.2 Render Staging Backend Environment Variables (`virtual-classroom-backend-staging`)
Configure in: **Render Dashboard -> `virtual-classroom-backend-staging` -> Environment**

| Environment Variable Name | Secret? | Recommended Staging Value / Source |
| :--- | :---: | :--- |
| `SPRING_PROFILES_ACTIVE` | No | `staging` |
| `PORT` | No | `8080` |
| `DATABASE_URL` | **Yes** | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | No | `vc_app_user.zrbpjcfssbtxxgrjtsca` |
| `DATABASE_PASSWORD` | **Yes** | Dedicated `vc_app_user` password (from local `.env`) |
| `JWT_SECRET` | **Yes** | Cryptographically strong Base64 >= 256-bit string |
| `JWT_EXPIRATION_MS` | No | `86400000` |
| `COOKIE_SECURE` | No | `true` |
| `COOKIE_SAME_SITE` | No | `None` |
| `CORS_ALLOWED_ORIGINS` | No | `https://virtual-classroom-staging.vercel.app` *(must match Vercel URL exactly)* |
| `STORAGE_PROVIDER` | No | `supabase` |
| `SUPABASE_URL` | No | `https://zrbpjcfssbtxxgrjtsca.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | **Yes** | Staging project `service_role` secret (from Supabase dashboard) |
| `SUPABASE_STORAGE_BUCKET` | No | `academic-assets` |
| `STAGING_ADMIN_BOOTSTRAP_ENABLED` | No | `false` *(temporarily set to `true` during one-time bootstrap)* |
| `STAGING_ADMIN_EMAIL` | No | Required if bootstrap enabled |
| `STAGING_ADMIN_PASSWORD` | **Yes** | Required if bootstrap enabled (>= 14 characters) |
| `STAGING_ADMIN_FULL_NAME` | No | `Staging Administrator` |

### 5.3 Vercel Staging Frontend Environment Variables (`virtual-classroom-staging`)
Configure in: **Vercel Project Settings -> Environment Variables** (Preview / Staging scope):

| Environment Variable Name | Secret? | Value |
| :--- | :---: | :--- |
| `VITE_API_BASE_URL` | No *(public)* | `<RENDER_STAGING_BACKEND_URL>/api/v1` (e.g. `https://virtual-classroom-backend-staging.onrender.com/api/v1`) |

---

## 6. Remaining Blockers Summary

1. **GitHub Environment `staging` Secrets Not Populated:** All 5 required secrets (`RENDER_STAGING_DEPLOY_HOOK_URL`, `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`, `STAGING_BACKEND_URL`) are missing from GitHub Settings.
2. **Render Staging Service Not Yet Provisioned:** `virtual-classroom-backend-staging` does not exist on Render (HTTP `404 Not Found`).
3. **Vercel Staging Project Not Yet Provisioned:** `virtual-classroom-staging` has no live deployment (HTTP `404 DEPLOYMENT_NOT_FOUND`).
4. **Step Output ID Defect in `staging-deployment.yml`:** Line 292 references `steps.resolve-urls.outputs.backend_url` instead of `steps.validate-urls.outputs.backend_url`.

---

## 7. Exact Manual Actions Required Before Live Deployment

1. **Fix Step Reference in `.github/workflows/staging-deployment.yml`:**
   Change line 292 from `steps.resolve-urls.outputs.backend_url` to `steps.validate-urls.outputs.backend_url` via a fast, targeted branch/PR into `develop`.
2. **Provision Render Staging Backend Service:**
   Create Web Service `virtual-classroom-backend-staging` from branch `develop` (Docker runtime, `./backend/Dockerfile`, `autoDeploy: false`, health check `/actuator/health/liveness`) and populate the environment variables listed in Section 5.2.
3. **Provision Vercel Staging Frontend Project:**
   Create project `virtual-classroom-staging` mapped to branch `develop` (Root directory: `frontend`, Framework: `Vite`, build command `npm run build`, output `dist`) and set `VITE_API_BASE_URL`.
4. **Populate GitHub Environment `staging` Secrets:**
   Add the 5 secrets listed in Section 5.1 to GitHub Environment `staging`.
5. **Execute Dry-Run Verification First:**
   Run `Staging Deployment` workflow with `dry_run_only=true` on `develop` to verify secret presence and permission gates before running live deployment.

---

## 8. Final Verdict

# **NOT READY**

**Verdict Rationalization:**  
While the application source code, database hardening, Spring security profiles, Docker packaging, and CI quality gates on `develop` (`c197d262afa647c2da500025031a421f082b20eb`) are in a **verified, high-integrity state**, staging deployment cannot proceed until:
1. The step output ID defect (`steps.resolve-urls` → `steps.validate-urls`) in `.github/workflows/staging-deployment.yml` is corrected.
2. The operator provisions the Render staging web service and Vercel staging project.
3. The operator populates the 5 required staging secrets in GitHub Environment `staging`.
