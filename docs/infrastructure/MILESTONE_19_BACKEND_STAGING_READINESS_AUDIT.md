# MILESTONE 19 — BACKEND STAGING DEPLOYMENT READINESS AUDIT (READ-ONLY)
## Virtual Classroom Platform — Pre-Deployment Gate Assessment

**Audit Date:** 2026-11-18  
**Audited Branch:** `feature/staging-readiness-config` (HEAD: `698abb6`)  
**Target Environment:** Render Staging (`virtual-classroom-backend-staging`) + Supabase Staging (`virtual-classroom-staging`, Ref: `zrbpjcfssbtxxgrjtsca`, AWS `ap-south-1`)  
**Production Boundary:** Production (`tzbjmxtmysvzxdmspiui`) and production deployment infrastructure strictly untouched and unaccessed  
**Execution Mode:** **STRICTLY READ-ONLY** — Zero database mutations, zero migrations, zero credential rotations, zero Render/Vercel deployments, zero branch merges, zero application code or config edits  

---

## 1. Executive Summary

### 1.1 Final Readiness Decision

> **DEPLOYMENT READINESS DECISION: `NOT READY — BLOCKERS FOUND`**

Although the **backend application artifact, database hardening, storage authorization, and GitHub Actions CI checks on `feature/staging-readiness-config` are technically sound**, the backend **cannot safely be deployed to the existing Render staging service right now** due to **three HIGH-severity deployment blockers** (zero CRITICAL blockers):

1. **HIGH BLOCKER (`FINDING-01`) — Hardened Code Not Merged into `develop`:** `render.yaml` (`branch: develop`) and `staging-deployment.yml` (`push.branches: [develop]`) deploy from `develop`, which is **18 commits behind** `feature/staging-readiness-config`. Triggering Render today would deploy obsolete `origin/develop` code missing `application-staging.yml`, `vc_app_user.zrbpjcfssbtxxgrjtsca`, disabled runtime Flyway, `/actuator/health/**` permitAll, `StorageAuthorizationService`, and hardened HikariCP/JWT configs.
2. **HIGH BLOCKER (`FINDING-02`) — Render Staging Secrets & Runtime Variables `NOT VERIFIED`:** Five `sync: false` secrets (`DATABASE_URL`, `DATABASE_PASSWORD` for `vc_app_user`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`) plus runtime variables (`DATABASE_USERNAME=vc_app_user.zrbpjcfssbtxxgrjtsca`, `CORS_ALLOWED_ORIGINS`) in the live Render Dashboard cannot be verified from Git and must be explicitly confirmed by the operator before deployment; missing or legacy values trigger intentional fail-fast startup crashes.
3. **HIGH BLOCKER (`FINDING-03`) — Unprotected GitHub `staging` Environment & Auto-Deploy Coupling on Merge to `develop`:** Live GitHub Environments API verified that `environments/staging` has `protection_rules: []` and `deployment_branch_policy: null`, its 5 deployment secrets are `NOT VERIFIED`, and merging `feature/staging-readiness-config` into `develop` will **automatically trigger** `.github/workflows/staging-deployment.yml` (deploying both Render AND Vercel) unless environment approval gates or secret pre-checks are configured first.

### 1.2 Summary Scorecard

| Audit Domain | Status | Notes |
| :--- | :---: | :--- |
| **A. Git & Repository Integrity** | **BLOCKED (`FINDING-01`)** | `feature/staging-readiness-config` (`698abb6`) is clean and synced with remote, containing all M18.3/M18.4 changes, but is 18 commits ahead of `origin/develop`. |
| **B. Backend Build & Runtime** | **VERIFIED PASS** | Java 21 / Spring Boot `3.4.3`, `./mvnw clean package` succeeded (`140` run, `134` passed, `0` failed, `6` Windows `embedded-pg` skipped), `Start-Class: com.virtualclassroom.VirtualClassroomApplication` verified in JAR manifest, all 11 YAML files valid. |
| **C. Supabase Staging Connectivity** | **VERIFIED IN CONFIG / LIVE SECRETS NOT VERIFIED** | Config enforces `vc_app_user.zrbpjcfssbtxxgrjtsca`, `sslmode=require`, HikariCP pooling + leak detection, disabled runtime Flyway, zero prod refs; live Render `DATABASE_URL`/`DATABASE_PASSWORD` values `NOT VERIFIED`. |
| **D. Authentication & Authorization** | **VERIFIED PASS (MINOR GAPS)** | Stateless JWT, BCrypt-12, staging fallback-secret rejection, HttpOnly/Secure/SameSite=None cookies, dual filter + `@PreAuthorize` RBAC, `/actuator/health/**` permitted; zero initial admin user in staging (`FINDING-04`) and no SMTP for forgot-password (`FINDING-06`). |
| **E. Storage Integration** | **VERIFIED IN CODE / LIVE HTTP NOT VERIFIED** | Private `academic-assets` bucket, official Supabase REST API (`Authorization` + `apikey`), domain-table `StorageAuthorizationService`, 14-MIME parity, 900s signed URLs, fail-closed `staging`/`prod`, zero frontend service-key refs; live Render-to-Supabase HTTP test `NOT VERIFIED` until Render secrets exist. |
| **F. Render Configuration** | **BLOCKED (`FINDING-02`)** | Unified `render.yaml` (`render-staging.yaml` does not exist) has valid Docker build, healthCheckPath `/actuator/health/liveness`, `autoDeploy: false`, and tenant-qualified staging user; live Render Dashboard secrets and actual region (`oregon` vs `ap-south-1`, `FINDING-07`) `NOT VERIFIED`. |
| **G. CI/CD & Deployment Safety** | **BLOCKED (`FINDING-03`)** | Latest CI Run `44286907059` (`2201000`): 4/4 jobs `SUCCESS`; `pr-validation.yml` never deploys; `production-release.yml` isolated to `main`/tags. However, `staging` environment lacks protection rules/branch policy, `staging-deployment.yml` auto-deploys on `develop` push without Render rollout polling (`FINDING-05`), and `integration-validation.yml` binds hardcoded `5432:5432` (`FINDING-08`). |
| **H. Application Integrations** | **CONDITIONAL (`FINDING-04`, `FINDING-06`)** | Zero dependency on demo users (`DevDataLoader` gated to `local`/`dev`); canonical curriculum (5 degrees, 9 courses, 8 subjects) seeded; requires out-of-band staging Admin bootstrap and notes absent SMTP delivery. |

---

## 2. Repository and Branch State

Inspected via read-only `git status`, `git branch -vv`, `git log`, and `git rev-list`:

- **Active Branch:** `feature/staging-readiness-config`
- **Local vs. Remote Sync:** Up to date with `origin/feature/staging-readiness-config` at commit `698abb6`.
- **Divergence from `origin/develop`:** `0` behind, **`18` commits ahead** (`git rev-list --left-right --count origin/develop...HEAD` -> `0   18`).
- **Latest Verified Commits Present on Branch:**
  - `698abb6` — `docs(milestone-18.4): record Option B CI gate split verification and green CI run 44286907059`
  - `2201000` — `ci(milestone-18.4): enforce blocking prod npm audit and report dev-dependency advisories as visible warnings`
  - `ad27c38` — `docs(milestone-18.4): document unpatched upstream picomatch CVE-2026-112366 dev-only advisory`
  - `cdef61d` — `docs(milestone-18.3): add staging security remediation implementation report`
  - `7cc23ac` — `security(milestone-18.3): harden staging storage, JWT, HikariCP, Flyway, Render config and DB role scripts`
  - `72810c0` — `docs(staging): record complete staging database inventory and final completion report`
  - `dc5cacd` — `fix(migration): configure baselineOnMigrate(false) and explicit public schema isolation for standalone runner`
- **Working Tree State:** Zero modified tracked files prior to creating this report. Eight legacy untracked root markdown reports (`milestone_13_3_*.md` through `milestone_16_*.md`) remain untracked locally and must **not** be staged or committed.
- **Branch Isolation:** Neither `develop` nor `main` was modified or merged.

---

## 3. Backend Build, Packaging & Test Evidence

### 3.1 Toolchain & Packaging Verification (`backend/pom.xml` & `backend/Dockerfile`)
- **Java Target:** Java `21` (`<java.version>21</java.version>`); Dockerfile builds on `eclipse-temurin:21-jdk-alpine` and runs on `eclipse-temurin:21-jre-alpine` as unprivileged user `appuser:appgroup` (UID/GID `10001`, `/sbin/nologin`).
- **Spring Boot Version:** `3.4.3` (`spring-boot-starter-parent`).
- **Executable JAR Manifest Inspection (`backend/target/virtual-classroom-backend-1.0.0.jar`):**
  ```text
  Main-Class: org.springframework.boot.loader.launch.JarLauncher
  Start-Class: com.virtualclassroom.VirtualClassroomApplication
  Spring-Boot-Version: 3.4.3
  ```
  *Verified:* Despite `<exec.mainClass>com.virtualclassroom.migration.StandaloneFlywayMigrationRunner</exec.mainClass>` for `exec-maven-plugin`, `spring-boot-maven-plugin:3.4.3:repackage` correctly resolves `Start-Class` to `com.virtualclassroom.VirtualClassroomApplication`.
- **Startup, Graceful Shutdown & Runtime Profile Precedence:**
  - `server.port: ${PORT:${SERVER_PORT:8080}}`, `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase: 20s`.
  - `backend/Dockerfile` sets default `ENV SPRING_PROFILES_ACTIVE=docker`, cleanly overridden by `render.yaml` (`SPRING_PROFILES_ACTIVE=staging`).
  - `application-staging.yml` sets `spring.flyway.enabled: false`, `spring.flyway.baseline-on-migrate: false`, `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false` (inherited from `application.yml`), and `server.error.include-message: never` / `include-binding-errors: never`.
  - **Actuator Exposure (`application.yml` + `SecurityConfig.java`):** Exposes `health,info,metrics`; `health.probes.enabled: true` (`/actuator/health/liveness`, `/actuator/health/readiness`); `health.show-details: when-authorized`. `SecurityConfig` permits `/actuator/health`, `/actuator/health/**`, and `/actuator/info` without authentication, while `/actuator/metrics` requires authentication.

### 3.2 Local Verification Results (Safe Read-Only Checks)
- **Maven Clean Package & Full Test Suite (`./mvnw clean package`):** **`BUILD SUCCESS`** (1m 15s, 134 main classes + 23 test classes compiled, repackaged JAR created).
  - **Total Tests:** `140` | **Passed:** `134` | **Failed:** `0` | **Errors:** `0` | **Skipped:** `6`
  - *Exact Skipped Tests:* 6 `embedded-pg` PostgreSQL tests (`FlywayMigrationSafetyTest` x2, `StandaloneFlywayMigrationRunnerTest` x4) skipped solely due to Windows host OS error=623 blocking `initdb.exe`; all 6 execute and pass on Linux in GitHub Actions CI.
- **Static YAML Syntax Validation:** All **11** YAML files (`render.yaml`, 6 `application*.yml` profiles, 4 `.github/workflows/*.yml` files) parsed cleanly via `yaml.safe_load`.
- **Local Docker Image Build:** **`NOT VERIFIED locally`** (`docker` CLI is not installed on the local Windows workstation). Verified remotely via GitHub Actions CI Run `44286907059` (`Docker Image Build & Compose Orchestration`: `SUCCESS`).

### 3.3 GitHub Actions CI Evidence
- **Latest Executed CI Run:** Run `44286907059` (`https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/44286907059`, Commit `2201000`, `2026-11-18T10:35:51Z`)
  - `Backend QA (JUnit 5, Mockito, Spring Security, Package)`: **`SUCCESS`**
  - `PostgreSQL 17 Migration & Standalone Runner Gate`: **`SUCCESS`**
  - `Frontend QA (TypeScript, Vitest, Build, Audit)`: **`SUCCESS`** *(blocking prod audit passed with 0 HIGH/CRITICAL; dev-only `CVE-2026-112366` visibly surfaced via check-run warning annotation)*
  - `Docker Image Build & Compose Orchestration`: **`SUCCESS`**
- *Note:* Commit `698abb6` only modified `docs/infrastructure/MILESTONE_18_4_FRONTEND_DEPENDENCY_SECURITY_REPORT.md`, which is covered by `paths-ignore: docs/**`, so Run `44286907059` represents the exact current code and workflow state.

---

## 4. Supabase Staging Connectivity Configuration Audit

| Configuration Check | Observed Repository State | Verification Status |
| :--- | :--- | :---: |
| **Staging JDBC URL Format** | `application-staging.yml` binds `${DATABASE_URL}`; `render.yaml` declares `DATABASE_URL` (`sync: false`). Expected staging value: `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`. | **Config Valid / Live Render Value `NOT VERIFIED`** |
| **Tenant-Qualified Runtime Username** | `render.yaml` line 24 declares `DATABASE_USERNAME: vc_app_user.zrbpjcfssbtxxgrjtsca`. | **VERIFIED IN `render.yaml` / Live Render Value `NOT VERIFIED`** |
| **Session Pooler & Port** | Port `5432` (Supavisor Session Pooler — compatible with PostgreSQL JDBC prepared statements `prepareThreshold: 5`). | **VERIFIED** |
| **SSL / TLS Enforcement** | `application-staging.yml` pins `spring.datasource.hikari.data-source-properties.sslmode: require` (enforces TLS even if `?sslmode=require` is omitted from `DATABASE_URL`). M18.3 verified `TLSv1.3` (`TLS_AES_256_GCM_SHA384`). | **VERIFIED** |
| **HikariCP Pool Sizing & Limits** | `maximum-pool-size: ${DB_POOL_MAX:10}`, `minimum-idle: ${DB_POOL_MIN:2}`, `idle-timeout: 300000`, `max-lifetime: 1200000`, `connection-timeout: 20000`, `validation-timeout: 5000`, `leak-detection-threshold: 30000`, `initialization-fail-timeout: 1`. Fits safely within `vc_app_user` `CONNECTION LIMIT 20` (supports zero-downtime rolling overlap of 2 Render instances). | **VERIFIED** |
| **Zero Runtime Migration Privileges Required** | `spring.flyway.enabled: false` and `baseline-on-migrate: false` in both `application-staging.yml` and `application-prod.yml`. M18.3 verified Hibernate `ddl-auto: validate` across all 21 JPA entities as `vc_app_user`. | **VERIFIED** |
| **Production Isolation** | Zero occurrences of production project ref `tzbjmxtmysvzxdmspiui` in staging configs, `render.yaml`, or `application-prod.yml`. Production `DATABASE_USERNAME` in `render.yaml` is `sync: false`. | **VERIFIED** |

---

## 5. Authentication and Authorization Review

- **JWT Signing & Validation (`JwtTokenProvider.java`):**
  - Enforces non-blank Base64 secret >= 256 bits (`32` bytes), HMAC-SHA signing, default expiry `86400000ms` (24h).
  - Explicitly throws `IllegalStateException` if `LOCAL_DEV_FALLBACK_SECRET` is active under `staging`, `prod`, or `production` profiles (verified by `JwtTokenProviderSecurityTest`).
- **Stateless Spring Security & Account Status Gating (`SecurityConfig.java`, `JwtAuthenticationFilter.java`):**
  - `SessionCreationPolicy.STATELESS`, CSRF disabled (appropriate for bearer JWT API; refresh cookie scoped to `/api/v1/auth` with `SameSite=None; Secure; HttpOnly` + strict CORS allowlist).
  - `BCryptPasswordEncoder(12)` password hashing.
  - Per-request database reload of `UserPrincipal` enforces `isEnabled()` (`status == APPROVED`) and `isAccountNonLocked()` (`status != SUSPENDED`), immediately revoking access on account suspension.
- **Public Endpoints Audit (`SecurityConfig.java` lines 85–103):**
  - Intentionally public: `/api/v1/auth/login`, `/api/v1/auth/register/**`, `/api/v1/auth/refresh`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`, `GET /api/v1/courses/**` (public catalog), `/actuator/health`, `/actuator/health/**`, `/actuator/info`.
  - `/api/v1/storage/files/**` is permitted ONLY when `app.storage.provider=local` (locked down in `staging`/`prod`).
  - `/api-docs/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` remain publicly permitted (`FINDING-09` — acceptable in staging, disable in prod).
- **CORS & Cross-Site Cookie Compatibility (`AuthController.java`, `SecurityConfig.java`, `render.yaml`, `frontend/src/core/api/client.ts`):**
  - `COOKIE_SECURE=true`, `COOKIE_SAME_SITE=None` (default in `application-staging.yml`), `HttpOnly=true`, `Path=/api/v1/auth`.
  - `SecurityConfig` sets `allowCredentials(true)` with explicit origin `https://virtual-classroom-staging.vercel.app` (no `*`, no `localhost`).
  - Frontend `apiClient` sets `withCredentials: true` (verified in `TokenSecurity.test.ts`).
  - *Potential Login/Refresh Flow Risk (`FINDING-05`):* If the Vercel staging frontend is accessed on an ephemeral preview URL (`https://<project>-<hash>.vercel.app`) instead of `https://virtual-classroom-staging.vercel.app`, the browser will reject CORS preflights and suppress refresh cookies.
  - *Operational Login Blocker for Initial Testing (`FINDING-04`):* Staging `public.users` has `0` rows (`DevDataLoader` is restricted to `local`/`dev`), so an initial `ROLE_ADMIN` account must be provisioned out-of-band before any registered user can be approved to log in.

---

## 6. Storage Integration Review

- **Private Bucket & Config (`application-staging.yml`, `render.yaml`, `FileStorageService.java`):**
  - Bucket pinned to `academic-assets` (`ALLOWED_BUCKETS` allowlist enforced).
  - Live staging bucket verified in M18.3 as `public = false`, `file_size_limit = 52428800` (50 MB), 14 exact `allowed_mime_types`.
  - `FileStorageService.ALLOWED_MIME_TYPES` matches the 14 staging bucket MIME types 1:1.
- **Official Supabase Storage REST API & Zero Public URLs:**
  - Uses `POST /storage/v1/object/sign/{bucket}/{path}` and `POST /storage/v1/object/upload/sign/{bucket}/{path}` with both `Authorization: Bearer` and `apikey` headers and `x-upsert: false`.
  - Default signed download URL expiry `900s` (clamped `60..3600s`).
  - Never constructs `/storage/v1/object/public/...` URLs.
- **Authorization & Fail-Closed Behavior:**
  - `StorageAuthorizationService` enforces fail-closed domain-table ownership/enrollment checks across `assignment_submissions`, `study_materials`, `assignments`, `profile-photos/*`, uploader namespace, and `ROLE_ADMIN`.
  - `StorageController.getLocalFile()` returns HTTP `403` when cloud storage is enabled.
  - `FileStorageService` fails fast at boot in `staging`/`prod` if `SUPABASE_URL` or `SUPABASE_SERVICE_ROLE_KEY` is blank, and throws `StorageOperationException` (HTTP `502` with sanitized message in `GlobalExceptionHandler`) on upstream errors.
- **Service-Role Key Isolation:**
  - Static search across `frontend/` confirmed **0 references** to `SUPABASE_SERVICE_ROLE_KEY`.
- **Verification Limitation:** Live HTTP round-trip from Render staging to `https://zrbpjcfssbtxxgrjtsca.supabase.co/storage/v1/...` is **`NOT VERIFIED`** until `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are configured in Render and the service is deployed.

---

## 7. Render Configuration Review

- **Blueprint Files Present:** `render.yaml` exists at repository root; `render-staging.yaml` does **not** exist (`FINDING-10`).
- **Staging Service Specification (`render.yaml` lines 6–42):**
  - `name`: `virtual-classroom-backend-staging`
  - `type`: `web`, `runtime`: `docker`, `dockerfilePath`: `./backend/Dockerfile`, `dockerContext`: `./backend`
  - `plan`: `starter`, `region`: `oregon` *(cross-continent latency vs. Supabase `ap-south-1` Mumbai — `FINDING-07`)*
  - `branch`: `develop` *(currently 18 commits behind `feature/staging-readiness-config` — **`FINDING-01` HIGH BLOCKER**)*
  - `healthCheckPath`: `/actuator/health/liveness` *(permitted in `SecurityConfig` on `feature/staging-readiness-config`, NOT permitted on current `develop`)*
  - `autoDeploy`: `false` *(safe manual/webhook-controlled deployments)*
- **Live Render Service State:** **`NOT VERIFIED`** (external dashboard state — service existence, actual region, connected branch, and all 5 `sync: false` secrets must be confirmed by operator in Render Dashboard prior to deployment — **`FINDING-02` HIGH BLOCKER**).

---

## 8. CI/CD Deployment Safety Review

| Workflow | Trigger | Deploys? | Safety Assessment |
| :--- | :--- | :---: | :--- |
| `pr-validation.yml` | `push` (`feature/staging-readiness-config`), `pull_request` (`develop`, `main`), `workflow_dispatch` | **NO** (`permissions: contents: read`) | Safe; strictly read-only QA & CI validation. |
| `integration-validation.yml` | `push` (`develop`), `pull_request` (`develop`), `workflow_dispatch` | **NO** (`permissions: contents: read`) | Safe, but binds hardcoded `5432:5432` on `ubuntu-latest` (`FINDING-08`). |
| `staging-deployment.yml` | **`push` (`develop`)** & `workflow_dispatch` | **YES** (Render staging hook + Vercel preview) | **Risks (`FINDING-03`, `FINDING-05`):** Auto-deploys both Render and Vercel on merge to `develop`; GitHub `staging` environment has `protection_rules: []` and `deployment_branch_policy: null`; does not wait for Render build/rollout before executing smoke tests; cannot target production URLs unless misconfigured in `staging` secrets. |
| `production-release.yml` | `push.tags` (`v*`), `workflow_dispatch` | **YES** (Production) | Properly isolated by `production-guard` requiring `main` ancestry and `production` environment; cannot be triggered by staging workflows. |

---

## 9. Missing / Unverified Configuration & Required Secret Names (NAMES ONLY)

*Strict Security Note: Zero secret values were retrieved, inspected, or printed.*

### 9.1 Render Staging Service (`virtual-classroom-backend-staging`) — Status: `NOT VERIFIED`
Operator must confirm all 5 secret variables AND verify 2 non-secret overrides match `render.yaml`:
1. `DATABASE_URL` *(secret / `sync: false`)* — Must be `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
2. `DATABASE_PASSWORD` *(secret / `sync: false`)* — Must be the **M18.3 `vc_app_user` password** (NOT the `postgres` migration password)
3. `JWT_SECRET` *(secret / `sync: false`)* — Cryptographically random Base64 key (>= 32 decoded bytes; never `LOCAL_DEV_FALLBACK_SECRET`)
4. `SUPABASE_URL` *(secret / `sync: false`)* — Must be `https://zrbpjcfssbtxxgrjtsca.supabase.co`
5. `SUPABASE_SERVICE_ROLE_KEY` *(secret / `sync: false`)* — Staging project `zrbpjcfssbtxxgrjtsca` service-role key ONLY
6. `DATABASE_USERNAME` *(verify synced to `vc_app_user.zrbpjcfssbtxxgrjtsca`, NOT legacy `postgres`)*
7. `CORS_ALLOWED_ORIGINS` *(verify exact match to live staging frontend origin, e.g. `https://virtual-classroom-staging.vercel.app`)*

### 9.2 GitHub Environment `staging` Secrets — Status: `NOT VERIFIED`
Required by `.github/workflows/staging-deployment.yml` (if CI-driven staging deployment is used):
1. `RENDER_STAGING_DEPLOY_HOOK_URL`
2. `VERCEL_TOKEN`
3. `VERCEL_ORG_ID`
4. `VERCEL_PROJECT_ID`
5. `STAGING_BACKEND_URL`
6. `STAGING_FRONTEND_URL` *(optional fallback if Vercel deploy output is absent)*

---

## 10. Risk Register

| ID | Severity | File & Configuration Key | Evidence Observed | Why It Matters | Required Remediation | Blocks Staging Deploy? |
| :-: | :---: | :--- | :--- | :--- | :--- | :---: |
| **F-01** | **HIGH** | `render.yaml` (`services[0].branch: develop`); `staging-deployment.yml` (`push.branches: [develop]`) | `origin/develop` is 18 commits behind `feature/staging-readiness-config` (`0 / 18`). | Deploying Render staging now builds obsolete `develop` code lacking `application-staging.yml`, `vc_app_user`, disabled Flyway, `/actuator/health/**` permitAll, and storage authz — causing boot/health failure. | Merge `feature/staging-readiness-config` into `develop` via reviewed PR (after securing `staging` env / Render secrets). | **YES** |
| **F-02** | **HIGH** | `render.yaml` (`DATABASE_URL`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` = `sync: false`; `DATABASE_USERNAME`) | Live Render Dashboard env vars are external (`NOT VERIFIED`). | Backend intentionally fails fast on startup if any secret is missing, uses dev JWT fallback, or still holds `postgres` credentials instead of `vc_app_user`. | Operator confirms all 5 secrets + `DATABASE_USERNAME=vc_app_user.zrbpjcfssbtxxgrjtsca` in Render Staging Dashboard before deployment. | **YES** |
| **F-03** | **HIGH** | GitHub Environment `staging` (`environments/staging`); `.github/workflows/staging-deployment.yml` (`on.push.branches: [develop]`) | Live GitHub API shows `protection_rules: []` and `deployment_branch_policy: null`; 5 GitHub secrets `NOT VERIFIED`; push to `develop` auto-triggers live deploy of Render + Vercel. | Merging PR to `develop` will immediately fire live deployment without manual approval, and any branch can invoke `workflow_dispatch` against `staging`. | Configure `deployment_branch_policy` (`develop` only) + required reviewer approval on GitHub Environment `staging` and verify the 5 secret names before merging to `develop`. | **YES** *(for CI-triggered deploy)* |
| **F-04** | **MEDIUM** | `DevDataLoader.java` (`@Profile({"local","dev"})`); staging `public.users` | `public.users` has `0` rows; all registrations start `PENDING`; no admin registration endpoint. | Backend boots cleanly, but no user can log in or approve registrations until an initial `ROLE_ADMIN` account exists. | Provision one initial staging `ROLE_ADMIN` (`APPROVED`, BCrypt-12 hash) out-of-band after backend boot. | **NO** *(blocks E2E auth)* |
| **F-05** | **MEDIUM** | `staging-deployment.yml` (lines 129–136, 156); `scripts/staging_smoke_tests.js` (lines 116, 163–170); `render.yaml` (`CORS_ALLOWED_ORIGINS`) | Webhook trigger does not wait for Render rollout; Vercel preview URL may differ from `https://virtual-classroom-staging.vercel.app`; smoke test accepts `404` on liveness and skips CORS header assertion. | Smoke tests may hit stale container or miss broken CORS; browser login/refresh-cookie fails if frontend domain mismatches `CORS_ALLOWED_ORIGINS`. | Verify stable Vercel staging domain matches `CORS_ALLOWED_ORIGINS`; add Render readiness poll and strict assertions in `staging_smoke_tests.js`. | **NO** |
| **F-06** | **MEDIUM** | `AuthService.java` (`forgotPassword`, lines 228–254); `backend/pom.xml` | Password reset token hashed/stored in DB, but no SMTP/email client exists to send `rawToken`. | Self-service password reset cannot deliver recovery emails in staging. | Track as known staging limitation; add transactional email integration in a follow-up milestone. | **NO** |
| **F-07** | **MEDIUM** | `render.yaml` (`services[0].region: oregon`) vs. Supabase `ap-south-1` | Oregon (US West) to Mumbai (`ap-south-1`) adds ~220–260ms RTT per SQL query. | Elevated API latency on multi-query endpoints and slower Hibernate startup validation. | Confirm existing Render service region (prefer `singapore` if creating/migrating) and align `render.yaml`. | **NO** |
| **F-08** | **MEDIUM** | `.github/workflows/integration-validation.yml` (line 42: `ports: [5432:5432]`) | Hardcoded host port `5432` conflicts with pre-installed PostgreSQL on `ubuntu-latest`. | PR to `develop` may trigger a flaky `integration-validation.yml` failure. | Switch `integration-validation.yml` to dynamic host port mapping (`- 5432` + `${{ job.services.postgres.ports['5432'] }}`). | **NO** |
| **F-09** | **LOW** | `SecurityConfig.java` (lines 90–93); `application-prod.yml` | Swagger UI / OpenAPI endpoints are `permitAll()` in all profiles. | Acceptable in staging; exposes API schema publicly in production if left enabled. | Set `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false` in `application-prod.yml` before production release. | **NO** |
| **F-10** | **LOW** | `render.yaml` (unified blueprint; no `render-staging.yaml`) | Single `render.yaml` defines both staging and prod services (`autoDeploy: false`). | Blueprint auto-sync must not inadvertently create/sync the prod service during staging work. | Manage staging service explicitly in Render Dashboard until production milestone is authorized. | **NO** |
| **F-11** | **LOW** | Local workstation (`docker` absent; 8 untracked root `milestone_13..16_*.md` files) | Local Docker build `NOT VERIFIED locally` (verified on CI Run `44286907059`); untracked legacy docs in working tree. | No impact on staging deployment; keep legacy untracked files out of commits. | Clean or archive untracked local docs separately when convenient. | **NO** |

---

## 11. Deployment Readiness Decision

**FINAL DECISION: `NOT READY — BLOCKERS FOUND`**

- **Critical Blockers:** `0`
- **High Blockers:** `3` (`F-01` branch merge to `develop`, `F-02` unverified Render staging secrets/env vars, `F-03` unprotected GitHub `staging` environment + auto-deploy on `develop` push)
- **Medium Findings:** `5` (`F-04`..`F-08`)
- **Low Findings:** `3` (`F-09`..`F-11`)

The backend code, packaging, Hibernate schema validation, database role hardening (`vc_app_user`), and CI quality gates are ready, **but** deployment MUST wait until the 3 HIGH operational/configuration blockers (`F-01`, `F-02`, `F-03`) are explicitly resolved by the operator.

---

## 12. Exact Prerequisites for the Next Milestone (Pre-Deployment Checklist)

Execute in this exact order before authorizing backend staging deployment:

1. **Harden GitHub Environment `staging` (`F-03`):**
   - In GitHub Repository Settings -> Environments -> `staging`, add **Required reviewers** (operator approval gate) and restrict **Deployment branches** strictly to `develop`.
   - Confirm whether all 5 GitHub `staging` secrets (`RENDER_STAGING_DEPLOY_HOOK_URL`, `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`, `STAGING_BACKEND_URL`) are configured if CI-driven staging deployment will be used.
2. **Configure & Verify Render Staging Service Secrets (`F-02`):**
   - In Render Dashboard for `virtual-classroom-backend-staging`, configure/verify:
     - `SPRING_PROFILES_ACTIVE=staging`
     - `PORT=8080`
     - `DATABASE_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
     - `DATABASE_USERNAME=vc_app_user.zrbpjcfssbtxxgrjtsca`
     - `DATABASE_PASSWORD=<vc_app_user 40-char staging password from local gitignored .env>`
     - `JWT_SECRET=<strong Base64 >= 256-bit staging secret>`
     - `COOKIE_SECURE=true`
     - `CORS_ALLOWED_ORIGINS=<exact live staging frontend origin, e.g. https://virtual-classroom-staging.vercel.app>`
     - `STORAGE_PROVIDER=supabase`
     - `SUPABASE_URL=https://zrbpjcfssbtxxgrjtsca.supabase.co`
     - `SUPABASE_SERVICE_ROLE_KEY=<staging service-role key>`
     - `SUPABASE_STORAGE_BUCKET=academic-assets`
3. **Optional Pre-Merge CI Hardening (`F-08`, `F-05`):**
   - Fix hardcoded `5432:5432` in `.github/workflows/integration-validation.yml` and harden Render rollout polling + CORS/liveness assertions in `staging-deployment.yml` / `scripts/staging_smoke_tests.js`.
4. **Merge `feature/staging-readiness-config` into `develop` (`F-01`):**
   - Open PR `feature/staging-readiness-config` -> `develop`, confirm all required checks pass, and merge.
5. **Deploy Backend to Render Staging & Run Live Verification:**
   - Trigger Render staging deploy (via Render Dashboard or approved `staging` environment run).
   - Verify `/actuator/health/liveness` (`200 UP`), `/actuator/health` (`200 UP`), `GET /api/v1/courses/degrees` (`200` with 5 degrees), unauthenticated `/api/v1/admin/metrics` (`401`), and live Supabase Storage upload + signed download + unauthorized `403` rejection.
6. **Provision Initial Staging Admin Account (`F-04`):**
   - Create one `APPROVED` `ROLE_ADMIN` user in staging out-of-band so full Student/Faculty/Admin login, approval, refresh-cookie, and storage E2E flows can be verified.

---

## 13. Audit Boundary Confirmation

- Zero modifications to Supabase staging or production databases.
- Zero access to production credentials or production infrastructure.
- Zero Render or Vercel deployments initiated.
- Zero merges into `develop` or `main`.
- Zero application source code, workflow, or deployment configuration modifications.
- Stopping here and awaiting Operator Review and explicit authorization for the next milestone.
