# STAGING END-TO-END TEST REPORT
## Virtual Classroom Platform — Separation of Local/CI Verified Evidence vs. Blocked Live Cloud E2E Tests

**Report Date:** 2026-11-18  
**Codebase Verified:** `origin/develop` (`036f0381ee459d807ab5857d13de17a74a8cfdfd`) + staging hardening branch `feature/staging-bootstrap-and-smoke-hardening`

> **HONESTY & EVIDENCE BOUNDARY:** Local and CI test suites validate application logic, security filters, PostgreSQL 17 migrations, frontend production builds, and component behavior. **They do NOT constitute live deployed cloud E2E verification.** Live deployed Render/Vercel E2E checks remain `BLOCKED` / `NOT TESTED` until staging services are provisioned and deployed.

---

## 1. Local & CI Automated Verification Results (Verified)

| Test Suite | Execution Environment | Exact Counts | Status |
| :--- | :--- | :--- | :---: |
| **Backend Maven Suite (`./mvnw test`)** | Local Windows (`openjdk-26.0.2`, post-hardening) | `147` run, **`141` passed**, `0` failures, `0` errors, **`6` skipped** *(Windows `embedded-pg` `initdb.exe` restriction in `FlywayMigrationSafetyTest` x2 and `StandaloneFlywayMigrationRunnerTest` x4)* | **PASS (with 6 documented Windows skips)** |
| **New Phase H Admin Bootstrap Tests (`StagingAdminBootstrapRunnerTest`)** | Local Windows (`./mvnw test`) | **`7/7` passed**, `0` failures, `0` skipped | **PASS** |
| **Backend Storage & Config Security Tests** | Local Windows (`./mvnw test`) | `StorageSecurityRemediationTest` (`11/11`), `StagingConfigurationRemediationTest` (`10/10`), `JwtTokenProviderSecurityTest` (`6/6`), `FullStackIntegrationEndToEndTest` (`8/8`) | **PASS** |
| **PostgreSQL 17 Migration & Standalone Runner Gate** | Linux GitHub Actions CI (`Run 44321518786`, Job `134861417174`) | All `6` live PostgreSQL 17 tests executed & passed against `postgres:17-alpine`, CLI fail-closed check passed, `psql` constraint & zero-demo-user checks passed | **PASS** |
| **Full-Stack Integration Verification** | Linux GitHub Actions CI (`Run 44349511111` on `develop` `036f0381ee45`, Job `134960953019`) | Frontend build + Vitest + Backend tests against `postgres:17-alpine` + JAR package + `psql` verification | **PASS** |
| **Docker Image Build & Compose Orchestration** | Linux GitHub Actions CI (`Run 44321518786`, Job `134861417363`) | Multi-stage backend/frontend images built & Compose verified *(Docker CLI not installed locally)* | **PASS (CI)** |
| **Frontend Build (`tsc -b && vite build`)** | Local Windows (`npm.cmd run build`) | `1887` modules transformed, `47` dist assets, `0` TypeScript errors, `0` secret/localhost leaks | **PASS** |
| **Frontend Vitest Suite (`npm.cmd test -- --run`)** | Local Windows | **`10/10` test files, `37/37` tests passed**, `0` failures, `0` skipped | **PASS** |
| **Staging Smoke Test Specification (`--dry-run`)** | Local Windows (`node scripts/staging_smoke_tests.js --dry-run`) | **`9/9` checks verified** in dry-run spec mode | **PASS (Dry-Run)** |
| **Static YAML Validation** | Local Windows (`yaml.safe_load_all`) | **`12/12` repository YAML files valid** | **PASS** |

---

## 2. Live Deployed Staging End-to-End Matrix (Phase G)

| E2E Domain | Live Staging Status | Blocking Dependency / Honest Notes |
| :--- | :---: | :--- |
| **Authentication (Login, Logout, JWT, Refresh Cookie, Invalid Credentials, Unauthorized)** | **BLOCKED** | Requires live Render staging backend + Vercel staging frontend + one-time Admin bootstrap (`0` users currently in staging DB). |
| **Student Workflows (Profile, Browse Courses, Enrollment, Assignments, Submissions, Exams, Forum, Notifications)** | **BLOCKED** | Requires live deployed Render + Vercel staging services and authorized test user accounts. |
| **Faculty Workflows (Profile, Assigned Courses, Assignment Creation, Grading, Exams, Results, Scheduled Live Sessions)** | **BLOCKED** | Requires live deployed Render + Vercel staging services. *(Note: Live sessions are scheduling & management records — no embedded WebRTC/video conferencing exists in the codebase.)* |
| **Admin Workflows (Dashboard Metrics, User Approvals, Role/Status Management, Courses & Subjects, Audit Logs)** | **BLOCKED** | Requires live deployed Render + Vercel staging services + operator execution of `StagingAdminBootstrapRunner`. |
| **Private File Storage Live E2E (Authorized Upload, Signed URL Download, Expiry, Ownership Denial, MIME/Size/Path Rejection)** | **BLOCKED** | Backend unit/security tests pass (`11/11`) and live Supabase anonymous public access is rejected (`HTTP 400 NoSuchBucket`); live authenticated upload/signed-URL E2E requires live Render staging backend with `SUPABASE_SERVICE_ROLE_KEY`. |
| **Password Reset Email Delivery (Phase I)** | **NOT IMPLEMENTED (UNAVAILABLE)** | `AuthService` generates 256-bit `SecureRandom` tokens, stores SHA-256 hashes with 15-min expiry and single-use replay protection, but **zero SMTP/email transport exists** (`spring-boot-starter-mail` is absent). Authenticated `POST /api/v1/auth/change-password` does not require SMTP. |
| **Responsive UI Live Browser Checks (Desktop, Tablet, Mobile, Console Errors, Nested Route Refresh)** | **BLOCKED** | Vercel SPA rewrites (`frontend/vercel.json`) and lazy-loaded role layouts (`App.tsx`, `RoleLayouts.test.tsx`) are verified statically/unit-tested; live browser verification requires deployed Vercel staging URL. |
