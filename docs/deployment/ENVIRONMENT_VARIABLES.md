# Environment Variables Specification

This document provides a comprehensive reference of all environment variables used across the Virtual Classroom Platform, spanning backend services, frontend applications, and Docker Compose orchestration.

---

## 1. Security & Storage Guidelines

- **Never Commit Secrets**: Never commit `.env` or `.env.local` files to version control. The repository `.gitignore` strictly ignores `.env*` while preserving `.env.example` templates.
- **Classification**:
  - **Public**: Safe to expose in client-side bundles or repository templates.
  - **Secret**: Must remain confidential. Exposing compromises database security or authentication tokens.
- **Rotation**: Regularly rotate `JWT_SECRET`, database passwords, and Supabase service role keys in production.

---

## 2. Master Environment Variable Reference

### 2.1 Backend Environment Variables (Spring Boot)

These variables configure the Spring Boot application (`backend/src/main/resources/application.yml` and `application-docker.yml`).

| Variable Name | Required | Default Value | Sensitivity | Purpose / Description |
| :--- | :---: | :--- | :---: | :--- |
| `SERVER_PORT` | No | `8080` | Public | The HTTP port the embedded Tomcat server binds to locally. |
| `PORT` | No | (mapped to `SERVER_PORT`) | Public | Dynamic port injected by container platforms like Render or Cloud Run. |
| `SPRING_PROFILES_ACTIVE` | No | `default` | Public | Active Spring profile (e.g., `default`, `docker`, `test`). |
| `SPRING_DATASOURCE_URL` | **Yes** | `jdbc:postgresql://localhost:5432/virtual_classroom` | Public | JDBC connection URL to PostgreSQL / Supabase database. |
| `DATABASE_URL` | No | (fallback for `SPRING_DATASOURCE_URL`) | Public | Alternative connection URL format supported in Docker profile. |
| `SPRING_DATASOURCE_USERNAME` | **Yes** | `postgres` | Public | Database username. |
| `SPRING_DATASOURCE_PASSWORD` | **Yes** | (none) | **Secret** | Database user password. |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | No | `org.postgresql.Driver` | Public | JDBC driver class name. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | No | `validate` | Public | Hibernate DDL generation mode. Set to `validate` in production. |
| `SPRING_FLYWAY_ENABLED` | No | `true` | Public | Whether Flyway database migrations run automatically on startup. |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE`| No | `true` | Public | Whether Flyway baselines an existing non-empty schema. |
| `JWT_SECRET` | **Yes** | (none) | **Secret** | Cryptographic secret for signing HMAC-SHA256 JWT tokens. Must be >= 32 characters. |
| `JWT_EXPIRATION_MS` | No | `900000` (15 min) | Public | Lifespan of access tokens in milliseconds. |
| `JWT_REFRESH_EXPIRATION_MS` | No | `604800000` (7 days) | Public | Lifespan of refresh tokens in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173,http://localhost:3000` | Public | Comma-delimited list of allowed CORS origins for browser API access. |
| `SUPABASE_URL` | **Yes** | (none) | Public | Base URL of the Supabase project (e.g. `https://xyz.supabase.co`). |
| `SUPABASE_API_KEY` | **Yes** | (none) | **Secret** | Supabase service-role or anon API key for Storage API calls. |
| `SUPABASE_STORAGE_BUCKET` | No | `academic-content` | Public | Supabase Storage bucket name for course files and submissions. |
| `SUPABASE_PUBLIC_URL_PREFIX` | No | `${SUPABASE_URL}/storage/v1/object/public` | Public | Base URL for accessing public files from Supabase Storage. |
| `LIVE_SESSION_DAILY_API_KEY` | No | (none) | **Secret** | Optional Daily.co API key for live video room orchestration. |
| `LIVE_SESSION_DAILY_DOMAIN` | No | (none) | Public | Optional Daily.co domain for live video room orchestration. |

---

### 2.2 Frontend Environment Variables (Vite + React)

All Vite frontend environment variables must start with the `VITE_` prefix to be exposed in the browser bundle.

| Variable Name | Required | Default Value | Sensitivity | Purpose / Description |
| :--- | :---: | :--- | :---: | :--- |
| `VITE_API_BASE_URL` | **Yes** | `/api` | Public | Target REST API base URL. Use `/api` when running behind Nginx or proxy, or `http://localhost:8080/api` in local dev. |
| `VITE_SUPABASE_STORAGE_PUBLIC_URL` | No | `${SUPABASE_PUBLIC_URL_PREFIX}` | Public | Public prefix for downloading static course attachments and user avatars. |

---

### 2.3 Docker Compose Environment Variables (`docker-compose.yml`)

These variables govern local multi-container orchestration.

| Variable Name | Required | Default Value | Sensitivity | Purpose / Description |
| :--- | :---: | :--- | :---: | :--- |
| `FRONTEND_PORT` | No | `80` | Public | Host port mapped to the Nginx frontend container (`${FRONTEND_PORT:-80}:80`). |
| `BACKEND_PORT` | No | `8080` | Public | Host port mapped to the Spring Boot backend container (`${BACKEND_PORT:-8080}:8080`). |
| `POSTGRES_PORT` | No | `5432` | Public | Host port mapped to the PostgreSQL container when running under `local-db` profile. |
| `POSTGRES_DB` | No | `virtual_classroom` | Public | Database name for local PostgreSQL service. |
| `POSTGRES_USER` | No | `postgres` | Public | Database user for local PostgreSQL service. |
| `POSTGRES_PASSWORD` | No | `postgres` | **Secret** | Password for local PostgreSQL service. |
| `BACKEND_MAX_RAM_PERCENTAGE` | No | `75.0` | Public | JVM flag tuning container memory limit percentage (`-XX:MaxRAMPercentage`). |
| `BACKEND_INITIAL_RAM_PERCENTAGE` | No | `50.0` | Public | JVM flag tuning initial heap allocation percentage (`-XX:InitialRAMPercentage`). |

---

## 3. Environment Example Files Inventory

The repository maintains standard template files across all scopes:

- **Root Template**: [`.env.example`](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/.env.example) (contains unified variables for Docker Compose and multi-service orchestration).
- **Backend Template**: [`backend/.env.example`](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/backend/.env.example) (contains all Spring Boot, Flyway, JDBC, Supabase, and JWT configuration).
- **Frontend Template**: [`frontend/.env.example`](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/frontend/.env.example) (contains Vite client variables).
- **Infrastructure Template**: [`infrastructure/.env.example`](file:///c:/Users/GS_Y/Downloads/New%20folder/Mine/Projects/Github/virtual-classroom/infrastructure/.env.example) (mirrors root compose environment variables for the infrastructure directory).
