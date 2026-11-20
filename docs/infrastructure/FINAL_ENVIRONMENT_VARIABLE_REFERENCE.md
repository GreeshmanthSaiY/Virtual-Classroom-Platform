# AUTHORITATIVE ENVIRONMENT VARIABLE REFERENCE
**Virtual Classroom Platform — Staging and Production Deployments**  
**Date:** 2026-11-20  
**Status:** RECONCILED & RELEASE-READY SPECIFICATION  
**Scope:** Backend (Render), Frontend (Vercel), CI/CD (GitHub Actions), Database (Supabase)

---

## 1. Overview & Security Principles

This document defines the complete, authoritative specification for all environment variables across all hosting layers of the Virtual Classroom Platform. It has been reconciled against the actual application configuration files (`render.yaml`, `application-staging.yml`, `application-prod.yml`, `FileStorageService.java`, and `.github/workflows/staging-deployment.yml`).

### Strict Tenant and Storage Identity
- **Staging Supabase Project Reference:** `zrbpjcfssbtxxgrjtsca` (`ap-south-1`)
- **Production Supabase Project Reference:** `tzbjmxtmysvzxdmspiui` (`ap-south-1`)
- **Canonical Storage Bucket:** `academic-assets` (Private bucket; allowlisted in `FileStorageService.java`)

### Strict Security Rules
1. **Frontend Isolation:** The frontend bundle is public client code. It MUST NEVER receive, embed, or reference:
   - Database connection strings or credentials (`DATABASE_*`, `JDBC_*`, `SPRING_DATASOURCE_*`, `POSTGRES_*`).
   - JWT signing secret (`JWT_SECRET`).
   - Supabase `service_role` secret (`SUPABASE_SERVICE_ROLE_KEY`).
   - Admin bootstrap credentials (`STAGING_ADMIN_*`).
2. **Backend Authoritative Control:** The backend Spring Boot service acts as the security boundary. All database queries, private file presigning, and role authorizations are server-evaluated.
3. **CORS Restrictions:** `CORS_ALLOWED_ORIGINS` must explicitly match the full scheme and domain of the authorized frontend (e.g., `https://virtual-classroom-staging.vercel.app`). Wildcards (`*`) are prohibited in staging and production.

---

## 2. Backend Environment Variables (Render Web Service)

These variables configure the Spring Boot container running on Render. Variable names align 1:1 with `render.yaml` and `backend/src/main/resources/application-staging.yml`.

| Variable Name | Required | Sensitivity | Staging Value / Format | Production Value / Format | Purpose & Architecture Role |
|---|:---:|:---:|---|---|---|
| `SPRING_PROFILES_ACTIVE` | **YES** | Public | `staging` | `prod` | Activates profile-specific configuration (`application-staging.yml` or `application-prod.yml`). |
| `PORT` | **YES** | Public | `8080` (or injected by Render) | `8080` (or injected by Render) | Server listening port. |
| `DATABASE_URL` | **YES** | Sensitive | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` | Supabase **Session Pooler** connection URL (IPv4-compatible port 6543, SSL required). |
| `DATABASE_USERNAME` | **YES** | Sensitive | `vc_app_user.zrbpjcfssbtxxgrjtsca` | `vc_app_user.tzbjmxtmysvzxdmspiui` | Dedicated least-privileged application user. Format for pooled connections: `<user>.<project_ref>`. |
| `DATABASE_PASSWORD` | **YES** | **HIGH SECRET** | *(Operator-managed strong password)* | *(Operator-managed strong password)* | Password configured for `vc_app_user` during database provisioning. |
| `JWT_SECRET` | **YES** | **HIGH SECRET** | *(Base64 or 32+ character hex secret)* | *(Base64 or 32+ character hex secret)* | HMAC-SHA secret used for signing and verifying JSON Web Tokens (minimum 256 bits). |
| `JWT_EXPIRATION_MS` | NO | Config | `86400000` (24 hours in ms) | `86400000` (24 hours in ms) | Access token validity duration in milliseconds. |
| `COOKIE_SECURE` | NO | Config | `true` | `true` | Enforces browser `Secure` flag on HttpOnly refresh token cookies over HTTPS. |
| `COOKIE_SAME_SITE` | NO | Config | `None` | `None` | Enables cross-origin cookie transport between Vercel and Render domains. |
| `CORS_ALLOWED_ORIGINS` | **YES** | Config | `https://virtual-classroom-staging.vercel.app` | `https://virtual-classroom.edu` | Comma-separated list of exact frontend origin URLs allowed to make cross-origin API calls. |
| `STORAGE_PROVIDER` | **YES** | Config | `supabase` | `supabase` | Activates Supabase private storage integration. |
| `SUPABASE_URL` | **YES** | Config | `https://zrbpjcfssbtxxgrjtsca.supabase.co` | `https://tzbjmxtmysvzxdmspiui.supabase.co` | Supabase project API gateway endpoint. Used by backend for storage presigning. |
| `SUPABASE_SERVICE_ROLE_KEY` | **YES** | **HIGH SECRET** | *(Staging service_role key)* | *(Prod service_role key)* | Backend-only key used exclusively for generating private signed URLs for private storage bucket objects. |
| `SUPABASE_STORAGE_BUCKET` | **YES** | Config | `academic-assets` | `academic-assets` | Target private storage bucket name (verified in `FileStorageService.java` allowlist). |
| `STAGING_ADMIN_BOOTSTRAP_ENABLED` | NO | Config | `true` (disable after initial boot) | *N/A (absent in prod)* | When `true`, `StagingAdminBootstrapRunner` checks and seeds the initial staging administrator account if none exists. |
| `STAGING_ADMIN_EMAIL` | Optional* | Sensitive | `staging-admin@virtualclassroom.internal` | *N/A* | Email for staging administrator bootstrap. *Required if bootstrap enabled. |
| `STAGING_ADMIN_PASSWORD` | Optional* | **HIGH SECRET** | *(Strong initial password)* | *N/A* | Initial password for staging administrator bootstrap. *Required if bootstrap enabled. |
| `STAGING_ADMIN_FULL_NAME` | NO | Config | `Staging Administrator` | *N/A* | Display name for bootstrapped administrator. |
| `SPRING_FLYWAY_ENABLED` | NO | Config | `false` | `false` | In staging/prod, schema migrations are pre-executed via Standalone Flyway Runner or Supabase dashboard; application startup migration remains `false`. |

---

## 3. Frontend Environment Variables (Vercel Project)

These variables configure the React/TypeScript/Vite client application on Vercel.

| Variable Name | Required | Sensitivity | Staging Value | Production Value | Description & Injection Method |
|---|:---:|:---:|---|---|---|
| `VITE_API_BASE_URL` | **YES** | Public | `https://virtual-classroom-backend-staging.onrender.com/api/v1` | `https://virtual-classroom-backend-prod.onrender.com/api/v1` | Full public URL of the backend REST API. Injected at Vite build time into client bundle. |

> **CRITICAL VERIFICATION:**  
> Run `npm run build` and scan `frontend/dist/assets/*.js`. Confirm that no `service_role` keys, JDBC strings, or internal passwords exist in the bundle.

---

## 4. GitHub Actions Staging Environment Secrets

Configured under: **GitHub Repository -> Settings -> Environments -> `staging` -> Environment Secrets**  
Exact secret names match `.github/workflows/staging-deployment.yml`:

| Secret Name | Required | Sensitivity | Target Workflow Consumer | Description |
|---|:---:|:---:|---|---|
| `RENDER_STAGING_DEPLOY_HOOK_URL` | **YES** | **SECRET** | `staging-deployment.yml` (Step: `deploy-render`) | Webhook URL generated in Render staging service settings to trigger an automated zero-downtime deployment. |
| `STAGING_BACKEND_URL` | **YES** | Public URL | `staging-deployment.yml` (Rollout poll & smoke tests) | Base public URL of Render backend (e.g., `https://virtual-classroom-backend-staging.onrender.com`). |
| `STAGING_FRONTEND_URL` | Optional | Public URL | `staging-deployment.yml` (Smoke tests fallback) | Base public URL of Vercel frontend (defaults to dynamic `vercel_url` output from deploy step). |
| `VERCEL_TOKEN` | **YES** | **SECRET** | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Personal Access Token generated under Vercel Account Settings -> Tokens. |
| `VERCEL_ORG_ID` | **YES** | Public ID | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Team / Account ID (`team_...` or personal account ID). |
| `VERCEL_PROJECT_ID` | **YES** | Public ID | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Project ID (`prj_...`) found in Vercel Project Settings. |

---

## 5. Local Development and Docker Compose Reference

For local testing via Docker Compose (`docker-compose.yml`), variables are loaded from `.env` (excluded by `.gitignore`):

```bash
# Local Docker Compose .env template
POSTGRES_DB=virtual_classroom
POSTGRES_USER=postgres
POSTGRES_PASSWORD=local_postgres_password
SPRING_PROFILES_ACTIVE=docker
DATABASE_URL=jdbc:postgresql://postgres:5432/virtual_classroom
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=local_postgres_password
JWT_SECRET=c2VjdXJlX2xvY2FsX2p3dF9zZWNyZXRfZm9yX2RldmVsb3BtZW50XzEyMzQ1Njc4OTA=
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
STORAGE_PROVIDER=local
STORAGE_LOCAL_DIR=./uploads
VITE_API_BASE_URL=http://localhost:8080/api/v1
```
