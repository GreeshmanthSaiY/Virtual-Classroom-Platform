# AUTHORITATIVE POST-DEPLOYMENT VERIFICATION RUNBOOK
**Virtual Classroom Platform — Staging Release Acceptance Suite**  
**Date:** 2026-11-20  
**Target Environments:** Render (Backend) + Vercel (Frontend) + Supabase (DB & Storage)  
**Execution Context:** Manual Operator or Automated CI Post-Deploy Phase

---

## 1. Overview & Verification Policy

This runbook defines the complete post-deployment testing procedure to validate that both the backend on Render and frontend on Vercel are operational, secure, properly integrated, and performing within acceptable boundaries.

### Safety Boundaries for Verification
1. **No Destructive Operations:** Never delete live student or course data.
2. **Synthetic / Disposable Test Data Only:** Any database writes during verification must create explicitly tagged records (e.g. `test-smoke-*`) and purge them immediately upon test completion.
3. **No 404 False Positives:** A 404 response on an endpoint must be treated as a test failure, never as proof of liveness.
4. **Security Barrier Verification:** Negative security tests (unauthorized access, bad credentials, cross-user file access) must fail closed with HTTP 401/403.

---

## 2. Automated Smoke Test Suite (`scripts/staging_smoke_tests.js`)

The repository includes a dedicated Node.js smoke testing script that validates live HTTP endpoints, CORS policies, actuator health, and security barriers without requiring third-party testing frameworks.

### 2.1 Execution Command

```bash
# Execute against live staging deployments
node scripts/staging_smoke_tests.js \
  --frontend=https://virtual-classroom-staging.vercel.app \
  --backend=https://virtual-classroom-staging.onrender.com
```

### 2.2 Dry-Run Verification (Pre-Flight)

```bash
# Validates script syntax, logic, and simulated pass criteria
node scripts/staging_smoke_tests.js --dry-run
```

### 2.3 Automated Checks Executed by the Suite

| # | Check Name | Target Endpoint | Method | Expected HTTP | Expected Body / Headers |
|---|---|---|:---:|:---:|---|
| 1 | Frontend Landing Page Response | `https://<frontend>/` | GET | 200 | Body contains `<div id="root">` |
| 2 | Frontend Login Route Response | `https://<frontend>/login` | GET | 200 | Body contains HTML (SPA rewrite verification) |
| 3 | Backend Actuator Liveness | `https://<backend>/actuator/health/liveness` | GET | 200 | JSON contains `{"status":"UP"}` |
| 4 | Backend Full Health Probe | `https://<backend>/actuator/health` | GET | 200 | JSON contains `{"status":"UP"}` |
| 5 | Protected Admin Barrier | `https://<backend>/api/v1/admin/metrics` | GET | 401 or 403 | Unauthenticated request rejected |
| 6 | Private Storage Barrier | `https://<backend>/api/v1/storage/files/forbidden.pdf` | GET | 401 or 403 | Unauthenticated file access blocked |
| 7 | Authentication Validation | `https://<backend>/api/v1/auth/login` | POST | 400 or 401 | Malformed credentials rejected |
| 8 | CORS Allowed Origin | `https://<backend>/api/v1/auth/login` | OPTIONS | 200 or 204 | `access-control-allow-origin` matches frontend URL |
| 9 | CORS Unauthorized Origin | `https://<backend>/api/v1/auth/login` | OPTIONS | 403 or no ACAO | Origin `https://evil-unauthorized-site.com` rejected |

---

## 3. Detailed Manual Verification Procedures

In addition to automated smoke testing, the release operator must perform the following domain-specific acceptance checks.

### 3.1 Backend Service Verification (Render)

#### Step 1: Health & Profile Check
```bash
curl -i https://<your-render-app>.onrender.com/actuator/health
```
- **Verify:** Status is HTTP 200 OK with `{"status":"UP"}`.
- **Verify:** Response time is < 500ms once warmed.

#### Step 2: Database Connectivity Check
```bash
curl -i https://<your-render-app>.onrender.com/actuator/health/readiness
```
- **Verify:** Returns HTTP 200 OK, confirming the application successfully established a pooler connection to Supabase via `vc_app_user`.

#### Step 3: Error Masking Check
```bash
curl -i https://<your-render-app>.onrender.com/api/v1/nonexistent-endpoint
```
- **Verify:** Returns standard structured JSON error (`GlobalExceptionHandler`).
- **Verify:** No Java stack traces, database schema names, or internal class paths are exposed.

---

### 3.2 Frontend Service Verification (Vercel)

#### Step 1: SPA Deep Linking & Asset Resolution
1. Open a browser in incognito mode.
2. Navigate directly to `https://<your-vercel-app>.vercel.app/login`.
3. **Verify:** The login screen loads without a 404 error (verifying `vercel.json` rewrites).
4. Inspect Network tab in Developer Tools:
   - **Verify:** All `.js` and `.css` assets return HTTP 200.
   - **Verify:** Favicon and images render cleanly.
   - **Verify:** Browser console shows zero unhandled runtime exceptions.

#### Step 2: Client API Base URL Inspection
1. In Developer Tools -> Console, inspect the network request triggered when clicking "Login".
2. **Verify:** Request URL starts with `https://<your-render-app>.onrender.com/api/v1/...`.
3. **Verify:** Request does not attempt to contact `localhost:8080`.

---

### 3.3 Authentication & Role-Based Access Control (RBAC)

#### Test Case A: Initial Administrator Login
1. Log in with the bootstrapped staging admin credentials:
   - Email: `staging-admin@virtualclassroom.internal` (or configured `STAGING_ADMIN_EMAIL`)
   - Password: `STAGING_ADMIN_INITIAL_PASSWORD`
2. **Verify:** Backend returns HTTP 200 with JWT access token and user profile containing role `ADMIN`.
3. **Verify:** Admin Dashboard renders user management and course management panels.

#### Test Case B: Role Boundary Enforcement
1. Extract the JWT token from the admin session.
2. Attempt to query endpoints across different roles:
   ```bash
   # Admin accessing admin endpoint -> Expected HTTP 200
   curl -H "Authorization: Bearer <ADMIN_JWT>" https://<backend>/api/v1/admin/users

   # Student accessing faculty-only or admin-only endpoints -> Expected HTTP 403 Forbidden
   curl -H "Authorization: Bearer <STUDENT_JWT>" https://<backend>/api/v1/admin/users
   ```
3. **Verify:** The server strictly enforces method-level `@PreAuthorize` security.

---

### 3.4 Supabase Private Storage & Signed URL Verification

#### Test Case A: Direct Public Access Rejection
1. Attempt to construct a direct Supabase Storage public URL:
   `https://tzbjmxtmysvzxdmspiui.supabase.co/storage/v1/object/public/classroom-materials/sample.pdf`
2. **Verify:** Returns HTTP 400/404 or `{"statusCode":"404","error":"Bucket not found"}` confirming the bucket is strictly private (`public = false`).

#### Test Case B: Backend-Authorized Signed URL Flow
1. As an authenticated user, request study materials or assignment submission download via backend:
   ```bash
   curl -H "Authorization: Bearer <JWT>" https://<backend>/api/v1/materials/1/download
   ```
2. **Verify:** Backend checks database permissions, verifies enrollment, and generates a short-lived signed URL (e.g. 15-minute expiry).
3. **Verify:** The signed URL successfully downloads the file when pasted into a browser.
4. **Verify:** An expired or tampered signed URL immediately fails with HTTP 400/403.

---

### 3.5 Database RLS & Runtime User Integrity

1. Connect to Supabase SQL editor using administrative credentials.
2. Verify runtime session limits and role properties:
   ```sql
   SELECT rolname, rolcanlogin, rolconnlimit, rolbypassrls
   FROM pg_roles
   WHERE rolname = 'vc_app_user';
   ```
   - **Verify:** `rolbypassrls = false`.
   - **Verify:** `rolconnlimit = 20`.
3. Verify table RLS status:
   ```sql
   SELECT tablename, rowsecurity
   FROM pg_tables
   WHERE schemaname = 'public';
   ```
   - **Verify:** `rowsecurity = true` across all application tables.

---

## 4. Rollback Criteria and Emergency Procedures

If any of the following critical failures occur during post-deployment verification:
- Backend fails to start or continuously crashes (OOM, DB connection pool exhausted).
- CORS headers completely block frontend communication.
- Database credentials or `service_role` keys are leaked in client assets.
- RLS bypass is detected.

### Immediate Rollback Procedure:
1. **Render (Backend):** In Render Dashboard -> Web Service -> Deploys, select the previous working deployment commit and click **Rollback**.
2. **Vercel (Frontend):** In Vercel Dashboard -> Deployments, find the previous stable deployment and click **Promote to Production**.
3. **Database:** Since migrations V1–V5 are backward-compatible, no database rollback is needed unless an emergency data corruption occurs, in which case execute `docs/infrastructure/sql/M18_3_ROLLBACK.sql`.
