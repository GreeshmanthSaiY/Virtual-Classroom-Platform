# Production Deployment Guide

This guide details the deployment of the Virtual Classroom Platform to production cloud environments:
- **Frontend**: Hosted on [Vercel](https://vercel.com)
- **Backend API**: Hosted as a Docker Web Service on [Render](https://render.com)
- **Database & Storage**: Managed on [Supabase](https://supabase.com) (PostgreSQL 16 & S3-compatible Object Storage)

---

## 1. Architecture Overview

```
                      +---------------------------------------+
                      |         End Users (Browser)           |
                      +-------------------+-------------------+
                                          |
                        +-----------------+-----------------+
                        |                                   |
                        v                                   v
             +---------------------+             +---------------------+
             |       Vercel        |             |       Render        |
             |   (Frontend SPA)    |             |    (Backend API)    |
             | - React 19 + Vite   |             | - Spring Boot 3.3.4 |
             | - vercel.json rules |             | - Java 21 (Temurin) |
             | - HTTPS Edge CDN    |             | - Dynamic $PORT     |
             +----------+----------+             | - Actuator Health   |
                        |                        +----------+----------+
                        | CORS Credentials                  |
                        +-----------------------------------+
                                                            |
                                                            | JDBC SSL
                                                            v
                                                 +---------------------+
                                                 |      Supabase       |
                                                 | - PostgreSQL 16 DB  |
                                                 | - Flyway Migrations |
                                                 | - Storage Bucket    |
                                                 +---------------------+
```

---

## 2. Supabase Cloud Configuration

### 2.1 Database Setup
1. Log in to [Supabase](https://supabase.com) and create or select your project.
2. Under **Project Settings -> Database**, retrieve your connection parameters:
   - **Host**: `db.<project-ref>.supabase.co`
   - **Database**: `postgres`
   - **Port**: `5432` (or `6543` for connection pooler in transaction mode)
   - **User**: `postgres`
   - **Password**: Your database password
3. Construct the JDBC Connection URL:
   ```
   jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
   ```
   *Note: Always append `sslmode=require` to enforce TLS encryption in transit.*

### 2.2 Storage Bucket Configuration
1. In the Supabase Dashboard, navigate to **Storage -> Buckets**.
2. Create a bucket named `academic-content` (or configure your custom bucket name).
3. Set the bucket to **Public** if public file download URLs are desired, or configure Row Level Security (RLS) policies for authenticated downloads.
4. Note your project URL and service role / anon API key from **Project Settings -> API**.

---

## 3. Render Backend Deployment

Render hosts the Spring Boot backend as a containerized Web Service.

### 3.1 Service Creation
1. Log in to [Render Dashboard](https://dashboard.render.com).
2. Click **New +** -> **Web Service**.
3. Connect your GitHub repository: `https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform.git`.
4. Configure service settings:
   - **Name**: `virtual-classroom-backend`
   - **Branch**: `update-local` (or `main` upon final release)
   - **Region**: Choose the region closest to your Supabase project (e.g., Oregon, Frankfurt).
   - **Runtime**: **Docker**
   - **Dockerfile Path**: `backend/Dockerfile` (or `infrastructure/docker/Dockerfile.backend`)
   - **Docker Context**: `backend` (or repository root if using root context)
   - **Instance Type**: Starter or Standard (minimum 1GB RAM recommended for JVM heap).

### 3.2 Dynamic Port & Health Check
- **Dynamic Port**: Render automatically injects the `PORT` environment variable (e.g. `PORT=10000`). The Spring Boot configuration in `application.yml` maps `server.port: ${PORT:${SERVER_PORT:8080}}`, so the application listens on Render's assigned port automatically.
- **Health Check Path**: Set health check path to `/actuator/health` in Render service settings.
- **Graceful Shutdown**: The backend is configured with `server.shutdown: graceful` and a 20-second drain window to ensure in-flight HTTP requests complete before container termination.

### 3.3 Backend Environment Variables on Render
Configure the following environment variables in the Render dashboard under **Environment**:

| Variable | Value / Description | Sensitive |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `docker` | No |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require` | No |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | No |
| `SPRING_DATASOURCE_PASSWORD` | `<your-database-password>` | **Yes** |
| `JWT_SECRET` | Strong secret (minimum 32 characters / 256 bits) | **Yes** |
| `JWT_EXPIRATION_MS` | `900000` (15 minutes) | No |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 days) | No |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.vercel.app,https://yourdomain.com` | No |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | No |
| `SUPABASE_API_KEY` | `<your-supabase-service-role-key>` | **Yes** |
| `SUPABASE_STORAGE_BUCKET` | `academic-content` | No |
| `SUPABASE_PUBLIC_URL_PREFIX` | `https://<project-ref>.supabase.co/storage/v1/object/public` | No |
| `SPRING_FLYWAY_ENABLED` | `true` | No |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `true` | No |

---

## 4. Vercel Frontend Deployment

Vercel provides edge hosting and automatic continuous deployments for the React Single Page Application.

### 4.1 Project Import
1. Log in to [Vercel](https://vercel.com).
2. Click **Add New...** -> **Project**.
3. Import your GitHub repository: `Virtual-Classroom-Platform`.
4. Configure Project Settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build` (runs `tsc -b && vite build`)
   - **Output Directory**: `dist`
   - **Install Command**: `npm ci`

### 4.2 Vercel Configuration (`frontend/vercel.json`)
The repository includes a production-ready `frontend/vercel.json` file configuring:
- **SPA Rewrites**: Directs all paths `/(.*)` to `/index.html` to allow client-side React Router navigation without 404 errors.
- **Security Headers**: Injects `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, and `Permissions-Policy`.

### 4.3 Frontend Environment Variables on Vercel
In the Vercel project dashboard under **Settings -> Environment Variables**, add:

| Variable | Value | Description |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | `https://virtual-classroom-backend.onrender.com/api` | Render backend API endpoint |
| `VITE_SUPABASE_STORAGE_PUBLIC_URL` | `https://<project-ref>.supabase.co/storage/v1/object/public` | Supabase public assets prefix |

---

## 5. Cross-Origin (CORS) & Cookie Alignment

Because Vercel (frontend) and Render (backend) reside on different domains in production:

1. **CORS Configuration**:
   Ensure `CORS_ALLOWED_ORIGINS` on Render contains the exact Vercel production URL:
   ```
   CORS_ALLOWED_ORIGINS=https://virtual-classroom-platform.vercel.app,https://yourdomain.com
   ```
   *Do not include a trailing slash.*

2. **HttpOnly Refresh Cookies**:
   - The backend delivers refresh tokens in HttpOnly cookies with `SameSite=None; Secure; Path=/api/auth/refresh`.
   - The frontend Axios client is configured with `withCredentials: true` to ensure the cookie is transmitted on cross-origin refresh requests.
   - The `Secure` flag is enforced in production environments, requiring HTTPS for both frontend and backend.

---

## 6. Post-Deployment Verification Checklist

After deploying to Render and Vercel, perform this smoke test sequence:

- [ ] **Backend Health**: Visit `https://your-backend.onrender.com/actuator/health`. Expected response: `{"status":"UP"}`.
- [ ] **Frontend Loading**: Visit `https://your-frontend.vercel.app/`. Confirm the landing page renders without console errors.
- [ ] **Deep Link Routing**: Navigate directly to `https://your-frontend.vercel.app/login` in an incognito window. Verify page renders without 404.
- [ ] **User Registration**: Register a new Student account. Confirm user record is created in Supabase PostgreSQL.
- [ ] **Login Flow**: Log in with credentials. Confirm access token is stored in memory and `refresh_token` cookie is set.
- [ ] **Token Refresh**: Trigger an authenticated action or wait for automatic silent refresh. Confirm refresh token rotates cleanly.
- [ ] **File Upload**: Upload an assignment or course material. Confirm asset is stored in Supabase Storage bucket `academic-content`.
- [ ] **Logout Flow**: Log out and verify refresh token is revoked on backend and cookie cleared in browser.
