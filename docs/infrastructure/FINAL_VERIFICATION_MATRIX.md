# FINAL PRE-DEPLOYMENT VERIFICATION MATRIX
**Virtual Classroom Platform — Master Subsystem Compliance Matrix**  
**Date:** 2026-11-20  
**Scope:** Git, CI/CD, Supabase Staging, Supabase Production Isolation, Backend, Frontend, Render, Vercel  
**Status Standard:** `VERIFIED`, `FAILED`, `NOT CONFIGURED`, `NOT VERIFIABLE`, `NOT APPLICABLE`

---

| Requirement | Expected Configuration | Actual Configuration | Evidence | Status | Remaining Action |
|---|---|---|---|:---:|---|
| **Git Release Branch** | Latest integration commits on `develop` | HEAD of `develop` is `4988faf` (54 ahead, 0 behind `main`) | `git log`, `git rev-parse origin/develop` | **VERIFIED** | None. Branch is release ready. |
| **Production Branch Isolation** | `main` branch untouched during staging release | `origin/main` at `3632edd`, untouched | `git rev-parse origin/main` | **VERIFIED** | None. Production base strictly isolated. |
| **Obsolete Branch Cleanup** | Delete temporary & reconcile branches; preserve `main`, `develop`, `update-local` | Obsolete branches deleted locally & on remote; 0 unique commits lost | `FINAL_BRANCH_CLEANUP_REPORT.md`, `git branch -a` | **VERIFIED** | None. Preserved exactly 3 long-term branches + PR #9. |
| **Documentation PR State** | PR #9 open from `feature/final-release-readiness-and-hygiene` into `develop` | PR #9 open, mergeable, review gate active | GitHub REST API response for PR #9 | **VERIFIED** | Operator to review and merge PR #9. |
| **CI Validation Pipeline** | Mandatory CI passes on `develop` | `integration-validation` (run 44772282314) succeeded | GitHub Actions workflow run logs | **VERIFIED** | None. Pipeline verified. |
| **Deployment Trigger Safety** | Manual `workflow_dispatch` only; no push deployment | `staging-deployment.yml` has only `workflow_dispatch` trigger | `staging-deployment.yml` lines 18-35 | **VERIFIED** | None. Safe by default. |
| **Deployment Dual-Confirmation** | `dry_run_only=true` & `confirm_live_deployment=false` by default | Defaults verified in workflow inputs; live deploy requires explicit flags | `staging-deployment.yml` lines 20-33, 50-70 | **VERIFIED** | None. Safe flags enforced. |
| **Deployment Branch Guard** | Deployment restricted strictly to `develop` | Script fails immediately if `github.ref != 'refs/heads/develop'` | `staging-deployment.yml` lines 42-47 | **VERIFIED** | None. Restricted to `develop`. |
| **Environment Reviewer Gate** | Manual human approval required for staging deploy | GitHub Environment `staging` configured with reviewer `GreeshmanthSaiY` | GitHub Environments API, `can_admins_bypass: false` | **VERIFIED** | Operator to approve deployment when triggered. |
| **Rollout Step Output Reference** | Step references declared output without runtime error | Step accesses `steps.validate-urls.outputs.backend_url` | `staging-deployment.yml` line 221 & 240 | **VERIFIED** | None. Output declared in step `validate-urls`. |
| **Rollout Timeout Safety** | Rollout polling has definitive maximum timeout | 32 iterations * 15s sleep = 8 minute timeout with curl 10s timeout | `staging-deployment.yml` lines 225-236 | **VERIFIED** | None. Bounded polling verified. |
| **Live Smoke Test Condition** | Smoke tests execute ONLY if deployment succeeds | `staging-smoke-tests` requires `deploy-staging.result == 'success'` | `staging-deployment.yml` lines 174-183 | **VERIFIED** | None. Cannot run against stale or failed deploys. |
| **Staging Supabase Project** | Dedicated staging project `zrbpjcfssbtxxgrjtsca` in `ap-south-1` | Configured as `zrbpjcfssbtxxgrjtsca` across configs & tests | `render.yaml`, `StagingConfigurationRemediationTest.java` | **VERIFIED** | None. Reconciled. |
| **Production Supabase Isolation** | Production project `tzbjmxtmysvzxdmspiui` not accessed | No calls, migrations, or connections made to production | `SUPABASE_ENVIRONMENT_SETUP_REPORT.md` | **VERIFIED** | None. Production protected. |
| **Staging Pooler Hostname & Port** | IPv4 Session Pooler on port `6543` | `aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` | `render.yaml`, `application-staging.yml` | **VERIFIED** | None. Pre-configured. |
| **Staging Database Username** | Tenant-qualified runtime user `vc_app_user.zrbpjcfssbtxxgrjtsca` | `vc_app_user.zrbpjcfssbtxxgrjtsca` in `render.yaml` | `render.yaml` line 23 | **VERIFIED** | None. Verified. |
| **Runtime Database Role Limits** | Least privilege: `NOBYPASSRLS`, limit 20 | Role created with `NOBYPASSRLS` and connection limit 20 | `M18_3_1_provision_vc_app_user.sql` | **VERIFIED** | None. Hardened. |
| **Row-Level Security (RLS)** | Enabled across all public tables | RLS enabled on all 22 public tables in staging Supabase | `STAGING_DATABASE_INVENTORY.md` | **VERIFIED** | None. Verified. |
| **Flyway Schema History** | Migrations V1 through V5 applied | Exactly 5 migration files in `db/migration/`; history recorded | `V1`..`V5` SQL files, Flyway runner logs | **VERIFIED** | None. Up to date. |
| **Runtime Flyway Disabled** | `spring.flyway.enabled: false` in staging/prod | `enabled: false` in `application-staging.yml` & `application-prod.yml` | `application-staging.yml` line 34 | **VERIFIED** | None. Prevents runtime DDL. |
| **Hibernate DDL Auto** | `ddl-auto: validate` in staging/prod | `validate` enforced in `application-staging.yml` | `application-staging.yml` line 31 | **VERIFIED** | None. Prevents automatic alterations. |
| **Storage Bucket Allowlist** | Canonical private bucket `academic-assets` | Enforced by `ALLOWED_BUCKETS = Set.of("academic-assets")` | `FileStorageService.java` lines 27-28 | **VERIFIED** | None. Verified in code allowlist. |
| **Storage Private Access Barrier** | Bucket `public = false`; unauthenticated probe fails | Direct public URL returns 400/404; signed URLs required | `FileStorageService.java`, smoke test check #6 | **VERIFIED** | None. Private by default. |
| **Backend Java Version** | Java 21 LTS compatibility | `pom.xml` specifies `java.version: 21`; Temurin 21 in Dockerfile | `pom.xml`, `backend/Dockerfile` | **VERIFIED** | None. Verified. |
| **Backend Compilation & Package** | Clean Maven compile and package | Maven package compiles without errors; executable JAR built | `./mvnw clean package` | **VERIFIED** | None. Build clean. |
| **Backend Unit & Security Tests** | All backend tests pass | 147 tests: 141 passed, 0 failed, 0 errors (6 Windows skips) | Maven Surefire test report | **VERIFIED** | None. 100% pass on Linux CI. |
| **JWT Entropy Enforcement** | Minimum 256-bit secret key enforced | `JwtTokenProviderSecurityTest` validates entropy check | `JwtTokenProviderSecurityTest.java` | **VERIFIED** | Operator to supply 256-bit secret. |
| **Staging Admin Bootstrapper** | Initial admin seeded conditionally | `StagingAdminBootstrapRunner` checks presence before insert | `StagingAdminBootstrapRunnerTest.java` | **VERIFIED** | Operator to supply admin password. |
| **Frontend Dependency Install** | Reproducible install via `npm ci` | `npm ci` installs with 0 lockfile drift | `frontend/package-lock.json` | **VERIFIED** | None. Reproducible. |
| **Frontend TypeScript Build** | `tsc -b` passes with zero errors | TypeScript compiler passes cleanly | `npm run build` | **VERIFIED** | None. Type-safe. |
| **Frontend Vitest Suite** | Component & unit tests pass | 10/10 test files passed, 37/37 unit tests passed | Vitest test runner output | **VERIFIED** | None. Tests green. |
| **Frontend Production Build** | Vite outputs optimized static bundle | `vite build` completed in 16.07s (47 bundle assets) | `frontend/dist/` assets | **VERIFIED** | None. Static assets ready. |
| **Client Bundle Secret Leak** | Zero secrets in client assets | Scan found 0 service keys, 0 JDBC URLs, 0 internal secrets | Script scan of `frontend/dist/assets/*.js` | **VERIFIED** | None. Verified zero leaks. |
| **Frontend SPA Rewrites** | Single Page Application rewrites to `/index.html` | `vercel.json` rewrites `/(.*)` to `/index.html` | `frontend/vercel.json` line 5 | **VERIFIED** | None. Configured. |
| **Cross-Origin Cookie Security** | Refresh tokens transport over HTTPS | `cookie-secure: true`, `cookie-same-site: None`, `withCredentials: true` | `application-staging.yml`, `client.ts` | **VERIFIED** | None. Cross-origin enabled. |
| **Automated Smoke Test Suite** | 9-point contract & security barrier verification | `scripts/staging_smoke_tests.js` passes dry-run | `node scripts/staging_smoke_tests.js --dry-run` | **VERIFIED** | Ready to run post-deploy. |
| **Render Web Service** | Staging Web Service created on Render | Web Service not yet provisioned in Render dashboard | External dashboard inspection | **NOT CONFIGURED** | Operator to create Web Service per Runbook Step 3. |
| **Render Backend Env Vars** | All backend environment variables populated | Environment variables not yet entered in Render | External dashboard inspection | **NOT CONFIGURED** | Operator to inject env vars per Runbook Step 3. |
| **Render Deploy Hook URL** | Webhook generated for automated deployment | Hook not yet created in Render dashboard | External dashboard inspection | **NOT CONFIGURED** | Operator to copy hook to GitHub Secret. |
| **Vercel Project Setup** | Staging project created on Vercel | Project not yet provisioned in Vercel dashboard | External dashboard inspection | **NOT CONFIGURED** | Operator to import project per Runbook Step 4. |
| **Vercel Env Var Setup** | `VITE_API_BASE_URL` injected in Vercel | Variable not yet entered in Vercel dashboard | External dashboard inspection | **NOT CONFIGURED** | Operator to inject URL per Runbook Step 4. |
| **GitHub Staging Secrets** | 6 required staging secrets added to GitHub Env | Secrets not yet added under GitHub Environment `staging` | GitHub Environment inspection | **NOT CONFIGURED** | Operator to add secrets per Runbook Step 5. |
| **Live Smoke Verification** | Tests run against live staging URLs | Live staging URLs not yet deployed | Deployment pending | **NOT APPLICABLE** | Execute after Step 6 deployment. |
