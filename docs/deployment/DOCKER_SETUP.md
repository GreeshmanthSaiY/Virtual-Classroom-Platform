# Docker Setup & Compose Orchestration Guide

This document provides instructions for containerizing, orchestrating, and operating the Virtual Classroom Platform using Docker and Docker Compose.

---

## 1. Prerequisites

Before running the platform with Docker, ensure the host machine meets the following requirements:

- **Docker Engine**: Version `24.0.0` or later
- **Docker Compose**: Version `v2.20.0` or later (Docker Compose v2 plugin `docker compose`)
- **System Memory**: Minimum 4 GB RAM available for container processes (8 GB recommended)
- **Disk Space**: At least 10 GB free space for Docker images, build caches, and volumes
- **Network Ports Available**:
  - `80` (or `3000`): Frontend web server & reverse proxy
  - `8080`: Spring Boot REST API
  - `5432`: Local PostgreSQL (only when using the `--profile local-db` profile)

---

## 2. Container Architecture

The application is containerized using multi-stage builds to minimize image size and harden security.

```
                           +---------------------------+
                           |       Host Browser        |
                           +-------------+-------------+
                                         |
                                         | HTTP (:80 / :3000)
                                         v
+-------------------------------------------------------------------------------+
| virtual-classroom-net (Bridge Network)                                       |
|                                                                               |
|  +--------------------+        /api/* proxy       +--------------------+      |
|  |     frontend       | ------------------------> |      backend       |      |
|  | (Nginx 1.27-alpine)|                           | (Temurin 21-jre)   |      |
|  | - Port 80          |                           | - Port 8080        |      |
|  | - Non-root nginx   |                           | - User: appuser    |      |
|  | - Healthz check    |                           | - Actuator Health  |      |
|  +--------------------+                           +---------+----------+      |
|                                                             |                 |
|                                                             | JDBC            |
|                                                             v                 |
|                                                   +--------------------+      |
|                                                   |     db (Optional)  |      |
|                                                   | (Postgres 16)      |      |
|                                                   | Profile: local-db  |      |
|                                                   +--------------------+      |
+-------------------------------------------------------------+-----------------+
                                                              |
                                           Alternative Default: Supabase Cloud DB
                                           (PostgreSQL + Supabase Storage)
```

### 2.1 Backend Container (`backend/Dockerfile` / `infrastructure/docker/Dockerfile.backend`)
- **Builder Stage**: `eclipse-temurin:21-jdk-alpine` builds the Spring Boot executable fat JAR via Maven Wrapper (`mvn clean package -DskipTests`).
- **Runtime Stage**: `eclipse-temurin:21-jre-alpine` provides a minimal execution environment (~150MB).
- **Security Hardening**:
  - Dedicated non-root user `appuser:appgroup` (`UID 10001`, `GID 10001`).
  - Read-only root filesystem with explicit permissions for `/app` and `/tmp`.
  - No shell root access.
- **JVM Optimizations**:
  - `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom`
- **Dynamic Port**: Binds to `${PORT:-8080}` to guarantee compatibility with container orchestrators such as Render.
- **Health Check**: Probes `/actuator/health/liveness` every 30s.

### 2.2 Frontend Container (`frontend/Dockerfile` / `infrastructure/docker/Dockerfile.frontend`)
- **Builder Stage**: `node:20-alpine` runs `npm ci` and `npm run build` using Vite to produce static assets in `/app/dist`.
- **Runtime Stage**: `nginx:1.27-alpine` acts as a high-performance HTTP server and reverse proxy.
- **Reverse Proxy**: Proxies `/api/` requests to `http://backend:8080/api/` with proper WebSocket and HTTP/1.1 forwarding headers.
- **SPA Fallback**: Configured with `try_files $uri $uri/ /index.html;` to support React Router deep linking.
- **Security Headers**: Injects `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, and `Permissions-Policy`.
- **Health Check**: Probes `/healthz` returning `200 OK`.

---

## 3. Configuration & Environment Files

Docker Compose reads environment variables from `.env` in the root directory.

1. **Create Root `.env`**:
   ```bash
   cp .env.example .env
   ```

2. **Configure Required Variables**:
   ```env
   # Database Configuration (Supabase PostgreSQL by default)
   SPRING_DATASOURCE_URL=jdbc:postgresql://db.<supabase-project-ref>.supabase.co:5432/postgres?sslmode=require
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=your-secure-db-password

   # JWT Secret (minimum 32 characters / 256 bits)
   JWT_SECRET=production_grade_jwt_secret_must_be_at_least_32_characters_long_for_security

   # Supabase Storage Integration
   SUPABASE_URL=https://<supabase-project-ref>.supabase.co
   SUPABASE_API_KEY=your-supabase-service-role-or-anon-key
   SUPABASE_STORAGE_BUCKET=academic-content
   SUPABASE_PUBLIC_URL_PREFIX=https://<supabase-project-ref>.supabase.co/storage/v1/object/public

   # Frontend API Base URL
   VITE_API_BASE_URL=/api
   ```

---

## 4. Launching the Services

### 4.1 Default Mode: Cloud Database (Supabase PostgreSQL)
By default, the platform relies on external Supabase PostgreSQL as verified in Milestones 1 through 6. The `db` service is isolated behind the `local-db` profile and will **not** be started.

```bash
# Build and start frontend and backend services
docker compose up --build -d
```

### 4.2 Local Database Mode: Local PostgreSQL Service
If you want to run completely offline without an external Supabase instance:

```bash
# Start frontend, backend, and the local PostgreSQL container
docker compose --profile local-db up --build -d
```

When running with `--profile local-db`, set `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/virtual_classroom` and `SPRING_DATASOURCE_PASSWORD=postgres` in your `.env`.

---

## 5. Verifying Operational Status

### 5.1 Service Status & Healthchecks
```bash
docker compose ps
```
Expected output:
```
NAME                         IMAGE                        COMMAND                  SERVICE      STATUS                    PORTS
virtual-classroom-backend    virtual-classroom-backend    "java -jar /app/app.…"   backend      Up (healthy)              0.0.0.0:8080->8080/tcp
virtual-classroom-frontend   virtual-classroom-frontend   "/docker-entrypoint.…"   frontend     Up (healthy)              0.0.0.0:80->80/tcp
```

### 5.2 Direct Health Endpoints Verification
- **Frontend Health**:
  ```bash
  curl -i http://localhost/healthz
  # Expected: HTTP/1.1 200 OK
  ```

- **Backend Health (Actuator)**:
  ```bash
  curl -i http://localhost:8080/actuator/health
  # Expected: {"status":"UP"}
  ```

- **Backend Liveness & Readiness**:
  ```bash
  curl -i http://localhost:8080/actuator/health/liveness
  curl -i http://localhost:8080/actuator/health/readiness
  # Expected: {"status":"UP"}
  ```

- **Reverse Proxy Test via Frontend**:
  ```bash
  curl -i http://localhost/api/actuator/health
  # Expected: {"status":"UP"} proxied through Nginx
  ```

---

## 6. Managing Logs and Containers

- **View All Logs**:
  ```bash
  docker compose logs -f
  ```

- **View Backend Logs Only**:
  ```bash
  docker compose logs -f backend
  ```

- **View Frontend Nginx Access & Error Logs**:
  ```bash
  docker compose logs -f frontend
  ```

- **Restart a Single Service**:
  ```bash
  docker compose restart backend
  ```

- **Stop All Containers Gracefully**:
  ```bash
  docker compose down
  ```

- **Stop All Containers and Remove Persistent Volumes (Data Reset)**:
  ```bash
  docker compose --profile local-db down -v
  ```

---

## 7. Troubleshooting

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| `backend` container status `unhealthy` | Database unreachable or Flyway migration failure | Inspect backend logs with `docker compose logs backend`. Check DB credentials, SSL flags (`sslmode=require`), and network connectivity. |
| `frontend` returns `502 Bad Gateway` on `/api/*` | Backend container is still initializing or crashed | Backend container takes ~15-25 seconds to complete Flyway migrations. Wait for `backend (healthy)` status before hitting APIs. |
| Port binding error (`Bind for 0.0.0.0:80 failed: port is already allocated`) | Another web server (IIS, Apache, previous container) is occupying port 80 | Edit `docker-compose.yml` to map host port `3000:80` for the frontend service. |
| JWT validation errors on startup | `JWT_SECRET` is missing or less than 32 characters | Check `.env` and supply a strong secret with 32+ characters. |
| Database migrations fail on local DB | Postgres container is starting up while backend attempts connection | Backend depends on `db` with `condition: service_healthy`. Ensure `pg_isready` healthcheck in compose succeeds. |
