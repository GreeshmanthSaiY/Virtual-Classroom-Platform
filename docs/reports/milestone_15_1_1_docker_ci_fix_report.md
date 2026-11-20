# Milestone 15.1.1 — Docker Compose Smoke Test CI Fix Report

**Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform`  
**Pull Request:** [#4 — ci(workflows): reconcile corrected CI workflows from main into develop](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/pull/4)  
**Target Base Branch:** `develop` (`a7c2126543be9adca0f0b40ded34acfdc8e4e389`)  
**Head Reconciliation Branch:** `reconcile/main-ci-into-develop` (`42bf4b4a24a80fb789168fe1b436b3ebbc336ea4`)  
**PR Status:** **OPEN and UNMERGED** (Mergeable: `clean`)  
**Audit Date:** November 14, 2026  

---

## 1. Executive Summary

In Milestone 15.1, Pull Request #4 was opened against `develop` to reconcile the corrected CI bootstrap workflows from `main`. While the unit testing, linting, migration, and build gates passed, the Docker Compose smoke test in job `Docker Image Build & Compose Orchestration` failed during container healthcheck verification.

In Milestone 15.1.1, a rigorous multi-stage root cause investigation was conducted without altering application code or introducing unwanted runtime dependencies. The root causes across database profiling, datasource configuration, token decoding, and container loopback resolution were resolved. As a result, **100% of all CI workflow runs and check runs on PR #4 have passed successfully**.

PR #4 remains **strictly open and unmerged** pending review.

---

## 2. Root Cause Analysis of the Original Failure

The failure of the Docker Compose smoke test stemmed from four distinct, interdependent technical factors:

### A. Absence of H2 Database Driver in the Production Image
* **Mechanism:** The previous workflow step attempted to run the production backend container against an in-memory H2 database using `DATABASE_URL: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"`.
* **Root Cause:** In `backend/pom.xml`, the `com.h2database:h2` dependency is scoped strictly to `<scope>test</scope>`. The production Docker image built by `backend/Dockerfile` executes `mvn clean package -DskipTests`, excluding test-scoped dependencies from the final runnable fat JAR (`app.jar`). Consequently, when the container launched with an H2 JDBC URL, the Spring Boot application threw `java.lang.ClassNotFoundException: org.h2.Driver` and terminated with a crash loop.
* **Architecture Alignment:** The backend is architected for PostgreSQL (Supabase in production, PostgreSQL 16 Alpine in local development). It must not depend on H2 in containerized production artifacts.

### B. Docker Compose Profile Requirement for Database Container
* **Mechanism:** In `docker-compose.yml`, the PostgreSQL service `db` is defined with `profiles: ["local-db"]`.
* **Root Cause:** Standard invocations such as `docker compose up -d` or `docker compose config` ignore services assigned to non-default profiles unless `--profile local-db` is explicitly passed. Without this profile flag, Docker Compose never spawned `db`, leaving port 5432 unavailable.

### C. JWT Secret Format Requirement
* **Mechanism:** When the container was re-routed to PostgreSQL, the Spring Boot application failed during context initialization with:
  ```
  Caused by: io.jsonwebtoken.io.DecodingException: Illegal base64 character: '_'
      at io.jsonwebtoken.io.Base64Decoder.decode(Base64Decoder.java:54)
      at com.virtualclassroom.security.JwtTokenProvider.getSigningKey(JwtTokenProvider.java:47)
  ```
* **Root Cause:** In `com.virtualclassroom.security.JwtTokenProvider.java:47`, the signing key is instantiated via `Decoders.BASE64.decode(secret)`. The previous dummy CI environment variable `ci-docker-smoke-test-secret-key-32bytes!` contained hyphens and an exclamation mark, which are illegal in standard RFC 4648 Base64. A valid Base64-encoded 256-bit string was mandatory.

### D. Frontend Healthcheck Loopback Target on Alpine Linux
* **Mechanism:** In `docker-compose.yml`, the frontend healthcheck command was defined as:
  ```yaml
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/healthz"]
  ```
* **Root Cause:** The frontend container is built upon `nginx:alpine`, which utilizes `musl libc`. When BusyBox `wget` attempts to resolve `localhost`, `musl libc` resolves `localhost` to the IPv6 loopback address `::1` first. However, `frontend/nginx.conf` was configured with `listen 80;`, binding exclusively to the IPv4 socket `0.0.0.0:80`. When `wget` attempted connection to `[::1]:80`, the kernel immediately issued `ECONNREFUSED` (`Connection refused`), marking the container `unhealthy` despite Nginx serving traffic normally.

---

## 3. Exact Workflow Changes to `.github/workflows/pr-validation.yml`

In `.github/workflows/pr-validation.yml`, the `docker-validation` job was corrected to adhere to the true multi-container architecture:

1. **Docker Compose Specification Validation:**
   Updated validation command to include the active profile:
   ```bash
   docker compose --profile local-db config
   ```
2. **Environment Variable Contract:**
   Replaced H2 parameters with genuine PostgreSQL connection variables and a valid Base64 JWT secret:
   ```yaml
   env:
     JWT_SECRET: "Y2ktZG9ja2VyLXNtb2tlLXRlc3Qtc2VjcmV0LWtleS0zMmJ5dGVzIQ=="
     SPRING_PROFILES_ACTIVE: "docker"
     DATABASE_URL: "jdbc:postgresql://db:5432/virtual_classroom"
     DATABASE_USERNAME: "postgres"
     DATABASE_PASSWORD: "postgrespassword"
     STORAGE_PROVIDER: "local"
   ```
3. **Deterministic Multi-Stage Startup Sequencing:**
   * **Stage 1 (Database Initialization):** Starts the database alone:
     ```bash
     docker compose --profile local-db up -d db
     ```
   * **Stage 2 (Database Health Gate):** Polls until PostgreSQL reports `(healthy)` via `pg_isready`:
     ```bash
     timeout 60 bash -c 'until docker compose --profile local-db ps db | grep -q "(healthy)"; do sleep 2; done'
     ```
   * **Stage 3 (Application Stack Startup):** Boots `backend` and `frontend` only after database health is guaranteed:
     ```bash
     docker compose --profile local-db up -d backend frontend
     ```
   * **Stage 4 (Full Stack Convergence):** Awaits all 3 containers reaching `(healthy)` status:
     ```bash
     timeout 120 bash -c 'until [ $(docker compose --profile local-db ps | grep -c "(healthy)") -ge 3 ]; do sleep 3; done'
     ```
   * **Stage 5 (Direct Endpoint Probing):** Directly probes `/actuator/health` on backend and `/healthz` on IPv4 loopback on frontend:
     ```bash
     docker compose --profile local-db exec -T backend wget -qO- http://localhost:8080/actuator/health
     docker compose --profile local-db exec -T frontend wget -qO- http://127.0.0.1:80/healthz
     ```
4. **Enhanced Diagnostic Cleanup Trap:**
   Configured a bash `trap cleanup EXIT` handler to capture container status (`docker compose ps`), logs from all services, and `docker inspect` health details if any step fails.

---

## 4. Essential Compose Changes to `docker-compose.yml`

### Diff:
```diff
diff --git a/docker-compose.yml b/docker-compose.yml
index e4945e8..48adcc0 100644
--- a/docker-compose.yml
+++ b/docker-compose.yml
@@ -70,7 +70,7 @@ services:
     networks:
       - virtual-classroom-net
     healthcheck:
-      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:80/healthz"]
+      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://127.0.0.1:80/healthz"]
       interval: 15s
       timeout: 5s
       retries: 3
```

### Explicit Justification:
* Changing `http://localhost:80/healthz` to `http://127.0.0.1:80/healthz` forces the healthcheck runner within Alpine Linux to probe IPv4 loopback (`127.0.0.1`), eliminating IPv6 `::1` resolution mismatch with Nginx's `listen 80;`.
* This fix is strictly minimal (1 line changed), requires zero modifications to Nginx configuration or build artifacts, and guarantees compatibility across both CI runner environments and local developer machines running Docker.

---

## 5. Database Service and Profile Configuration

| Attribute | Configuration Value |
|---|---|
| **Service Name** | `db` |
| **Profile** | `local-db` |
| **Container Name** | `virtual-classroom-local-db` |
| **Container Image** | `postgres:16-alpine` |
| **Exposed Port** | `5432:5432` |
| **Database Name** | `virtual_classroom` |
| **Username** | `postgres` |
| **Password** | `postgrespassword` |
| **Healthcheck Command** | `pg_isready -U postgres -d virtual_classroom` |
| **Health Parameters** | Interval: 10s, Timeout: 5s, Retries: 5, Start Period: 10s |

---

## 6. Environment Variable Contract

The backend container executes with the following runtime contract during CI smoke validation:

```env
JWT_SECRET=Y2ktZG9ja2VyLXNtb2tlLXRlc3Qtc2VjcmV0LWtleS0zMmJ5dGVzIQ==
SPRING_PROFILES_ACTIVE=docker
DATABASE_URL=jdbc:postgresql://db:5432/virtual_classroom
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgrespassword
STORAGE_PROVIDER=local
```

* **Flyway Execution:** Under the `docker` Spring profile, Flyway automatically runs on container startup, executing migrations `V1__init_schema.sql` through `V5__create_course_recommendations_and_analytics.sql`.
* **Zero Mocking:** The smoke test operates against real PostgreSQL 16 schema tables with foreign keys and constraints intact.

---

## 7. Local Validation & Verification Evidence

1. **YAML Syntax Validation:**
   ```
   docker-compose.yml: VALID YAML
   .github/workflows/pr-validation.yml: VALID YAML
   .github/workflows/integration-validation.yml: VALID YAML
   ```
2. **Git Diff Hygiene:**
   `git diff --check` reported zero trailing whitespace, carriage return, or formatting anomalies.
3. **Changed Files Count in PR #4:**
   Exactly 2 files modified (`.github/workflows/pr-validation.yml` and `docker-compose.yml`). Zero application source, build tool, or dependency files touched.

---

## 8. PR #4 Current Status & Commit Record

* **Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform`
* **Pull Request:** [PR #4](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/pull/4)
* **Title:** `ci(workflows): reconcile corrected CI workflows from main into develop`
* **Base Branch:** `develop` (`a7c2126543be9adca0f0b40ded34acfdc8e4e389`)
* **Head Branch:** `reconcile/main-ci-into-develop`
* **Latest Head Commit SHA:** `42bf4b4a24a80fb789168fe1b436b3ebbc336ea4`
* **Mergeable State:** `clean` (Mergeable: `true`)
* **State:** **`OPEN`** (Unmerged: `true`)

### Commit History on Reconciliation Branch:
| Commit SHA | Author | Summary |
|---|---|---|
| `42bf4b4` | Greeshmanth Sai | `ci: fix frontend healthcheck loopback target to 127.0.0.1 in docker-compose` |
| `420e5e8` | Greeshmanth Sai | `ci: fix JWT_SECRET base64 encoding for Docker Compose smoke test` |
| `4eb0035` | Greeshmanth Sai | `ci: activate local-db profile and PostgreSQL health check for Docker smoke test` |
| `7eb87df` | Greeshmanth Sai | `ci: reconcile corrected CI workflows from main into develop` |
| `3632edd` | Greeshmanth Sai | `Merge pull request #3 from GreeshmanthSaiY/ci/bootstrap-workflows` |

---

## 9. GitHub Actions Verification & Job Conclusions

For the latest commit `42bf4b4a24a80fb789168fe1b436b3ebbc336ea4`, both triggered workflows and all 5 check runs completed with status **`success`**:

### Workflow 1: Pull Request Validation
* **Run URL:** [https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255)
* **Event:** `pull_request` (target: `develop`)
* **Conclusion:** **`success`** :white_check_mark:

| Job Name | Status | Conclusion | Job URL |
|---|---|---|---|
| **Frontend QA (TypeScript, Vitest, Build, Audit)** | Completed | **`success`** | [Job 132434579947](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255/job/132434579947) |
| **Backend QA (JUnit 5, Mockito, Spring Security, Package)** | Completed | **`success`** | [Job 132434580083](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255/job/132434580083) |
| **PostgreSQL 16 Migration & Integrity Gate** | Completed | **`success`** | [Job 132434580108](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255/job/132434580108) |
| **Docker Image Build & Compose Orchestration** | Completed | **`success`** | [Job 132434580123](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699255/job/132434580123) |

### Workflow 2: Integration Validation
* **Run URL:** [https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699262](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699262)
* **Event:** `pull_request` (target: `develop`)
* **Conclusion:** **`success`** :white_check_mark:

| Job Name | Status | Conclusion | Job URL |
|---|---|---|---|
| **Full-Stack Integration Verification** | Completed | **`success`** | [Job 132434579950](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43590699262/job/132434579950) |

### Execution Excerpt from Docker Smoke Test Log (Job 132434580123):
```
=== 1. Starting PostgreSQL service (db) ===
Container virtual-classroom-local-db Started 
=== 2. Waiting for PostgreSQL to report healthy ===
virtual-classroom-local-db   postgres:16-alpine   Up 6 seconds (healthy)   0.0.0.0:5432->5432/tcp
=== 3. Starting backend and frontend services ===
Container virtual-classroom-backend Healthy 
Container virtual-classroom-frontend Started 
=== 4. Waiting for all services to report healthy ===
=== 5. Active Container Status ===
virtual-classroom-backend    virtual-classroom-backend:latest    Up 22 seconds (healthy)   0.0.0.0:8080->8080/tcp
virtual-classroom-frontend   virtual-classroom-frontend:latest   Up 6 seconds (healthy)    0.0.0.0:3000->80/tcp
virtual-classroom-local-db   postgres:16-alpine                  Up 28 seconds (healthy)   0.0.0.0:5432->5432/tcp
=== 6. Probing Service Health Endpoints ===
{"status":"UP","groups":["liveness","readiness"]}
healthy
Docker Compose smoke test completed successfully.
=== Cleaning up Docker Compose resources ===
```

---

## 10. Confirmation of Strict Operational Constraints

1. **No Application Dependencies or Code Modified:**
   No changes were made to `backend/pom.xml`, `backend/src/**`, `frontend/package.json`, or `frontend/src/**`. Specifically, H2 was not added to production dependencies.
2. **No Migrations Modified:**
   Flyway migration scripts `V1` through `V6` remain untouched.
3. **No Deployments Triggered:**
   No deployment workflows were run. Render, Vercel, and Supabase production environments remain isolated and unaffected.
4. **Protected Branches Untouched:**
   No direct pushes were made to `main`, `develop`, or `update-local`. All work was performed on `reconcile/main-ci-into-develop`.
5. **PR #4 Remains Open and Unmerged:**
   PR #4 was not merged. It is in a completely clean, passing state ready for human review and sign-off.
