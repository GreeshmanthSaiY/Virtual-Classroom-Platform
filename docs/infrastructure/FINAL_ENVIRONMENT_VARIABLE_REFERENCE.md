# AUTHORITATIVE ENVIRONMENT VARIABLE REFERENCE
**Virtual Classroom Platform — Staging and Production Deployments**  
**Date:** 2026-11-20  
**Status:** RELEASE-READY SPECIFICATION  
**Scope:** Backend (Render), Frontend (Vercel), CI/CD (GitHub Actions), Database (Supabase)

---

## 1. Overview & Security Principles

This document defines the complete, authoritative specification for all environment variables across all hosting layers of the Virtual Classroom Platform.

### Strict Security Rules
1. **Frontend Isolation:** The frontend bundle is public client code. It MUST NEVER receive, embed, or reference:
   - Database connection strings or credentials (`JDBC_*`, `SPRING_DATASOURCE_*`, `POSTGRES_*`).
   - JWT signing secret (`JWT_SECRET`).
   - Supabase `service_role` secret (`SUPABASE_SERVICE_ROLE_KEY`).
   - Admin bootstrap credentials (`STAGING_ADMIN_*`).
2. **Backend Authoritative Control:** The backend Spring Boot service acts as the security boundary. All database queries, private file presigning, and role authorizations are server-evaluated.
3. **CORS Restrictions:** `CORS_ALLOWED_ORIGINS` must explicitly match the full scheme and domain of the authorized frontend (e.g., `https://virtual-classroom-staging.vercel.app`). Wildcards (`*`) are prohibited in staging and production.

---

## 2. Backend Environment Variables (Render Web Service)

These variables configure the Spring Boot container running on Render.

| Variable Name | Required | Sensitivity | Default / Example Value | Description & Operational Source |
|---|:---:|:---:|---|---|
| `SPRING_PROFILES_ACTIVE` | **YES** | Public | `staging` (or `prod`) | Activates profile-specific configuration (`application-staging.yml`). |
| `PORT` | **YES** | Public | `10000` (Render default) or `8080` | Server listening port injected dynamically by Render or configured manually. |
| `JDBC_DATABASE_URL` | **YES** | Sensitive | `jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require` | Supabase **Session Pooler** connection URL (IPv4-compatible port 6543, SSL required). Obtain from Supabase Project Settings -> Database -> Connection Pooling. |
| `JDBC_DATABASE_USERNAME` | **YES** | Sensitive | `vc_app_user.tzbjmxtmysvzxdmspiui` | Dedicated least-privileged application user provisioned in Milestone 18.3. Format for pooled connections: `<user>.<project_ref>`. |
| `JDBC_DATABASE_PASSWORD` | **YES** | **HIGH SECRET** | *(Operator-managed strong password)* | Password configured for `vc_app_user` during database user provisioning. |
| `JWT_SECRET` | **YES** | **HIGH SECRET** | *(Base64 or 32+ character hex secret)* | HMAC-SHA secret used for signing and verifying JSON Web Tokens. Must be at least 256 bits (32 bytes). |
| `JWT_EXPIRATION` | NO | Config | `86400000` (24 hours in ms) | Access token validity duration in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | **YES** | Config | `https://virtual-classroom-staging.vercel.app` | Comma-separated list of exact frontend origin URLs allowed to make cross-origin API calls. |
| `SUPABASE_URL` | **YES** | Config | `https://tzbjmxtmysvzxdmspiui.supabase.co` | Supabase project API gateway endpoint. Used by backend for private storage presigning. |
| `SUPABASE_SERVICE_ROLE_KEY` | **YES** | **HIGH SECRET** | *(Supabase service_role JWT key)* | Backend-only key used exclusively for generating private signed URLs for private storage bucket objects. Obtain from Supabase Project Settings -> API. |
| `SUPABASE_STORAGE_BUCKET` | **YES** | Config | `classroom-materials` | Target private storage bucket name verified in Supabase Storage. |
| `STAGING_ADMIN_BOOTSTRAP_ENABLED` | NO | Config | `true` (disable after initial boot) | When `true`, `StagingAdminBootstrapRunner` checks and seeds the initial staging administrator account if none exists. |
| `STAGING_ADMIN_EMAIL` | Optional* | Sensitive | `staging-admin@virtualclassroom.internal` | Email for staging administrator bootstrap. *Required if bootstrap enabled. |
| `STAGING_ADMIN_INITIAL_PASSWORD` | Optional* | **HIGH SECRET** | *(Strong initial password)* | Initial password for staging administrator bootstrap. *Required if bootstrap enabled. |
| `STAGING_ADMIN_FULL_NAME` | NO | Config | `Staging Administrator` | Display name for bootstrapped administrator. |
| `SPRING_FLYWAY_ENABLED` | NO | Config | `false` | In staging/prod, schema migrations are pre-executed via Standalone Flyway Runner or Supabase dashboard; application startup migration should remain `false`. |

---

## 3. Frontend Environment Variables (Vercel Project)

These variables configure the React/TypeScript/Vite client application on Vercel.

| Variable Name | Required | Sensitivity | Default / Example Value | Description & Injection Method |
|---|:---:|:---:|---|---|
| `VITE_API_BASE_URL` | **YES** | Public | `https://virtual-classroom-staging.onrender.com/api/v1` | Full public URL of the backend REST API. Injected at Vite build time into client bundle. |

> **CRITICAL VERIFICATION:**  
> Run `npm run build` and scan `frontend/dist/assets/*.js`. Confirm that no `service_role` keys, JDBC strings, or internal passwords exist in the bundle.

---

## 4. GitHub Actions Staging Environment Secrets

Configured under: **GitHub Repository -> Settings -> Environments -> `staging` -> Environment Secrets**

| Secret Name | Required | Sensitivity | Target Workflow Consumer | Description |
|---|:---:|:---:|---|---|
| `RENDER_DEPLOY_HOOK_URL` | **YES** | **SECRET** | `staging-deployment.yml` (Step: `deploy-render`) | Webhook URL generated in Render service settings to trigger an automated zero-downtime deployment. |
| `RENDER_API_KEY` | Optional | **SECRET** | Fallback deploy / status polling | Render account API key for programmatic status checks. |
| `RENDER_SERVICE_ID` | Optional | Public ID | Render service identifier | Service ID (e.g. `srv-c...`) for Render API calls. |
| `VERCEL_TOKEN` | **YES** | **SECRET** | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Personal Access Token generated under Vercel Account Settings -> Tokens. |
| `VERCEL_ORG_ID` | **YES** | Public ID | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Team / Account ID (`team_...` or personal account ID). |
| `VERCEL_PROJECT_ID` | **YES** | Public ID | `staging-deployment.yml` (Step: `deploy-vercel`) | Vercel Project ID (`prj_...`) found in Vercel Project Settings. |
| `BACKEND_STAGING_URL` | **YES** | Public URL | `staging-deployment.yml` (Rollout poll & smoke tests) | Base public URL of Render backend (e.g., `https://virtual-classroom-staging.onrender.com`). |
| `FRONTEND_STAGING_URL` | **YES** | Public URL | `staging-deployment.yml` (Smoke tests) | Base public URL of Vercel frontend (e.g., `https://virtual-classroom-staging.vercel.app`). |

---

## 5. Local Development and Docker Compose Reference

For local testing via Docker Compose (`docker-compose.yml`), variables are loaded from `.env` (excluded by `.gitignore`):

```bash
# Local Docker Compose .env template
POSTGRES_DB=virtual_classroom
POSTGRES_USER=postgres
POSTGRES_PASSWORD=local_postgres_password
SPRING_PROFILES_ACTIVE=docker
JDBC_DATABASE_URL=jdbc:postgresql://postgres:5432/virtual_classroom
JDBC_DATABASE_USERNAME=postgres
JDBC_DATABASE_PASSWORD=local_postgres_password
JWT_SECRET=c2VjdXJlX2xvY2FsX2p3dF9zZWNyZXRfZm9yX2RldmVsb3BtZW50XzEyMzQ1Njc4OTA=
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
VITE_API_BASE_URL=http://localhost:8080/api/v1
```
