# Milestone 16.1 — Staging Configuration Remediation Report

**Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform`  
**Pull Request:** [PR #5 — fix(staging): remediate staging profile, storage bucket allowlist, and cross-site cookies](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/pull/5)  
**Target Base Branch:** `develop` (`2c7f36bd2dd147f733543937304ddbe0468cca6e`)  
**Feature Branch:** `feature/staging-readiness-config` (`64a3e1d0fa82ca4342fc87c9ae17706da5083220`)  
**PR Status:** **OPEN and UNMERGED** (Mergeable: `clean`)  
**Execution Date:** November 14, 2026  

---

## 1. Exact Files Changed

A total of 9 files were modified or created to resolve the three staging configuration defects:

| File Path | Change Type | Purpose |
|---|---|---|
| `backend/src/main/resources/application-staging.yml` | **New File** | Complete Spring Boot staging profile contract (PostgreSQL datasource, Flyway, Hikari pool, Supabase storage). |
| `backend/src/main/resources/application.yml` | **Modified** | Added base default `cookie-same-site: ${COOKIE_SAME_SITE:Lax}` to `app.security`. |
| `backend/src/main/resources/application-prod.yml` | **Modified** | Added `cookie-same-site: ${COOKIE_SAME_SITE:None}` to `app.security` for HTTPS cross-site support. |
| `backend/src/main/java/com/virtualclassroom/controller/AuthController.java` | **Modified** | Implemented environment-aware refresh-cookie policy: `SameSite=None; Secure` for HTTPS, `SameSite=Lax` for HTTP. |
| `backend/src/main/java/com/virtualclassroom/security/SecurityConfig.java` | **Modified** | Trimmed whitespace on allowed CORS origins and added `Origin`, `X-Token-Transport`, `X-Client-Type` to allowed headers. |
| `backend/src/test/java/com/virtualclassroom/StagingConfigurationRemediationTest.java` | **New File** | 9 automated unit tests verifying staging profile parsing, bucket allowlist, cookie attributes, and CORS policies. |
| `backend/src/test/java/com/virtualclassroom/BackendSecurityAuditTest.java` | **Modified** | Updated existing mock test constructor call to explicitly verify `SameSite=Strict` behavior when configured. |
| `render.yaml` | **Modified** | Aligned staging and production `SUPABASE_STORAGE_BUCKET` values to canonical name `academic-assets`. |
| `infrastructure/render/render-staging.yaml` | **Modified** | Aligned staging blueprint `SUPABASE_STORAGE_BUCKET` value to `academic-assets`. |

---

## 2. Staging Profile Configuration (`application-staging.yml`)

The newly created `backend/src/main/resources/application-staging.yml` establishes the staging environment contract without hardcoding any secrets:

```yaml
# ==============================================================================
# Virtual Classroom Platform - Staging Environment Configuration Profile
# ==============================================================================
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: ${DB_POOL_MAX:10}
      minimum-idle: ${DB_POOL_MIN:2}
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
  security:
    cookie-secure: ${COOKIE_SECURE:true}
    cookie-same-site: ${COOKIE_SAME_SITE:None}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:https://virtual-classroom-staging.vercel.app,http://localhost:5173}
  storage:
    provider: ${STORAGE_PROVIDER:supabase}
    supabase:
      url: ${SUPABASE_URL}
      key: ${SUPABASE_SERVICE_ROLE_KEY}
      bucket: ${SUPABASE_STORAGE_BUCKET:academic-assets}
```

### Architectural Guarantees:
* **Fail-Fast Datasource:** Requires `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. Fails fast on startup if database credentials are not supplied.
* **Schema Safety:** Enforces `hibernate.ddl-auto: validate` so Hibernate cannot modify tables directly; Flyway retains exclusive schema migration ownership.
* **SSL Enforcement:** Honors SSL configuration specified in the JDBC URL (`sslmode=require`).

---

## 3. Storage Bucket Configuration & Allowlist Alignment

### Defect Identified in Milestone 16:
`render.yaml` and `render-staging.yaml` specified `academic-assets-staging`. However, `FileStorageService.java:43` defines a strict allowlist:
```java
public static final Set<String> ALLOWED_BUCKETS = Set.of(
        "academic-assets",
        "study-materials",
        "submissions",
        "profile-photos",
        "course-documents"
);
```
Passing `academic-assets-staging` triggered an immediate `IllegalArgumentException` on container startup.

### Remediation Implemented:
* Aligned `render.yaml` (staging and prod services) and `infrastructure/render/render-staging.yaml` to specify `value: academic-assets`.
* The staging bucket will reside in a dedicated Staging Supabase project, achieving complete infrastructure-level isolation while adhering to the application allowlist.
* Signed URL generation, private bucket access, and path sanitization remain intact.

---

## 4. Cross-Site Refresh-Cookie & CORS Security Changes

### A. Environment-Aware Cookie Configuration in `AuthController.java`
* **Defect:** `refreshToken` cookies were previously hardcoded with `SameSite="Strict"`. When frontend (`*.vercel.app`) and backend (`*.onrender.com`) operate on different registrable domains, modern browsers block `SameSite=Strict` cookies on cross-site requests, breaking `/api/v1/auth/refresh`.
* **Fix:** `AuthController` now dynamically inspects `cookieSecure` and optional `cookieSameSite`:
  ```java
  if (cookieSameSite != null && !cookieSameSite.isBlank()) {
      this.cookieSameSite = cookieSameSite.trim();
  } else {
      // When HTTPS is active (cookieSecure=true), default to "None" for cross-site deployment.
      // When HTTP is active (cookieSecure=false), default to "Lax" (browsers reject SameSite=None without Secure).
      this.cookieSameSite = cookieSecure ? "None" : "Lax";
  }
  ```
* **Attribute Parity on Logout:** In `clearRefreshTokenCookie`, the cleared cookie (`Max-Age=0`) retains identical `Path=/api/v1/auth`, `Secure`, `HttpOnly`, and `SameSite` flags, ensuring the browser reliably purges the cookie.
* **Security Controls Retained:**
  - `HttpOnly: true` prevents JavaScript access (XSS defense).
  - Raw refresh token is suppressed from JSON responses for browser clients.
  - In-memory access token storage remains unchanged in `frontend/src/core/api/client.ts`.

### B. CORS Configuration in `SecurityConfig.java`
* Trimmed whitespace from comma-separated origins in `CORS_ALLOWED_ORIGINS` to avoid CORS rejection from configuration formatting.
* Added `Origin`, `X-Token-Transport`, and `X-Client-Type` to `allowedHeaders`.
* Preserved `allowCredentials: true` with strict non-wildcard origin matching.

---

## 5. Automated Tests Added & Local Execution Results

### A. Dedicated Test Suite: `StagingConfigurationRemediationTest.java`
9 automated unit tests were created and passed cleanly:
1. `stagingProfile_ConfigurationFile_ValidatesContract` — **PASS**
2. `storageService_BucketAllowlist_AcceptsCanonicalStagingBucket` — **PASS**
3. `storageService_BucketAllowlist_RejectsSuffixedBucket` — **PASS**
4. `storageService_BucketAllowlist_AcceptsAllStandardBuckets` — **PASS**
5. `authController_CrossSiteStagingCookie_ConfiguresSameSiteNoneAndSecure` — **PASS**
6. `authController_LocalDevelopmentCookie_ConfiguresSameSiteLaxWithoutSecure` — **PASS**
7. `authController_RefreshTokenRotation_EmitsMatchingCrossSiteCookie` — **PASS**
8. `authController_Logout_ClearsCookieWithMatchingAttributes` — **PASS**
9. `securityConfig_CorsCredentials_AllowsExactOriginsAndHeaders` — **PASS**

### B. Full Backend Test Suite Execution
* **Command:** `.\mvnw.cmd test` (with Java 21)
* **Result:** **113 tests run, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS in 01:31 min).
* Included Zonky embedded PostgreSQL Flyway migration safety tests, JWT provider tests, authorization tests, and all service suites.

### C. Full Frontend Vitest Execution
* **Command:** `npm test -- --run`
* **Result:** **10 test files passed (100%), 37 tests passed (37/37)**.

---

## 6. Frontend Production Build Results

* **Command:** `npm run build` (`tsc -b && vite build`)
* **Output:**
  - `dist/index.html` (0.91 kB)
  - `dist/assets/index-o6OQyfqf.css` (61.18 kB)
  - `dist/assets/index-DeXIFLV1.js` (347.12 kB)
* **Build Time:** 7.98s
* **Result:** **Clean production build with zero TypeScript compilation errors**.

---

## 7. CI Workflow Runs & Required Checks Status (PR #5)

All 5 required checks configured in the `develop-protection` ruleset passed with **`success`**:

### Workflow 1: Pull Request Validation (Run ID: 43594888447)
* **URL:** [https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447)
* **Conclusion:** **`success`** :white_check_mark:

| Check / Job Name | Status | Conclusion | Job URL |
|---|---|---|---|
| **Frontend QA (TypeScript, Vitest, Build, Audit)** | Completed | **`success`** | [Job 132447171729](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447171729) |
| **Backend QA (JUnit 5, Mockito, Spring Security, Package)** | Completed | **`success`** | [Job 132447172079](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447172079) |
| **PostgreSQL 16 Migration & Integrity Gate** | Completed | **`success`** | [Job 132447172015](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447172015) |
| **Docker Image Build & Compose Orchestration** | Completed | **`success`** | [Job 132447172037](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888447/job/132447172037) |

### Workflow 2: Integration Validation (Run ID: 43594888434)
* **URL:** [https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888434](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888434)
* **Conclusion:** **`success`** :white_check_mark:

| Check / Job Name | Status | Conclusion | Job URL |
|---|---|---|---|
| **Full-Stack Integration Verification** | Completed | **`success`** | [Job 132447171789](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/runs/43594888434/job/132447171789) |

---

## 8. Commit SHA & Pull Request Details

* **Repository:** `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform`
* **Pull Request:** [PR #5](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/pull/5)
* **Title:** `fix(staging): remediate staging profile, storage bucket allowlist, and cross-site cookies`
* **Base Branch:** `develop` (`2c7f36bd2dd147f733543937304ddbe0468cca6e`)
* **Head Branch:** `feature/staging-readiness-config`
* **Head Commit SHA:** `64a3e1d0fa82ca4342fc87c9ae17706da5083220`
* **Mergeable:** `True` (State: `clean`)
* **Status:** **`OPEN` and `UNMERGED`**

---

## 9. Remaining Cloud Prerequisites (User Action Required)

The repository configuration is now 100% remediated. The following external cloud infrastructure steps remain for the user before triggering a live staging deployment:

1. **Supabase Staging Project:**
   - Create a dedicated staging project on Supabase.
   - Note the JDBC Session Pooler URL (port `5432`) and service role key.
   - Create private storage bucket named `academic-assets`.
2. **Render Staging Web Service:**
   - Create the staging service using `render.yaml` blueprint linked to branch `develop`.
   - Populate dashboard environment variables (`DATABASE_URL`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`).
   - Copy the generated Render deploy hook URL.
3. **Vercel Staging Project:**
   - Link project on Vercel with build command and set `VITE_API_BASE_URL` to the Render staging backend URL.
   - Generate Vercel deployment token and note Org/Project IDs.
4. **GitHub Environment Secrets:**
   - In GitHub Repository Settings $\rightarrow$ Environments $\rightarrow$ `staging`, configure the 5 secrets:
     * `RENDER_STAGING_DEPLOY_HOOK_URL`
     * `VERCEL_TOKEN`
     * `VERCEL_ORG_ID`
     * `VERCEL_PROJECT_ID`
     * `STAGING_BACKEND_URL`

---

## 10. Confirmation of Operational Restrictions

* **No Direct Push to Protected Branches:** All commits were authored on `feature/staging-readiness-config`. `main` and `develop` were untouched.
* **PR #5 Remains Unmerged:** PR #5 was created and verified with all 5 passing checks, but remains unmerged awaiting review.
* **Zero Deployments Triggered:** Preflight guards prevented any staging or production deployments.
* **No Secrets or Credentials Exposed:** All credentials follow environment variable contracts.
