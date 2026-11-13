# Local Development Guide

This guide walks developers through setting up, running, testing, and debugging the Virtual Classroom Platform in a local development environment.

---

## 1. System Requirements

Ensure your workstation has the following tools installed:

| Tool | Version Requirement | Verification Command |
| :--- | :--- | :--- |
| **Java JDK** | 21 (Eclipse Temurin or OpenJDK 21) | `java -version` |
| **Node.js** | 20.x LTS or higher | `node -v` |
| **npm** | 10.x or higher | `npm -v` |
| **Git** | 2.40+ | `git --version` |
| **Database** | PostgreSQL 16+ or Supabase Cloud Project | `psql --version` |
| **Docker** (Optional) | Docker Engine 24+ & Docker Compose v2 | `docker compose version` |

---

## 2. Repository Checkout & Branch Setup

Clone the repository and switch to the active development branch:

```bash
git clone https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform.git
cd Virtual-Classroom-Platform
git checkout update-local
```

---

## 3. Environment Configuration

The application uses environment variables for both backend and frontend components.

### 3.1 Backend Configuration (`backend/.env`)
Copy the backend sample environment file:
```bash
cp backend/.env.example backend/.env
```

Edit `backend/.env` with your credentials:
```env
# Server Port
SERVER_PORT=8080

# Database Configuration (Supabase PostgreSQL or Local PostgreSQL)
SPRING_DATASOURCE_URL=jdbc:postgresql://db.<supabase-project-ref>.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-secure-password
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver

# JPA & Flyway
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true

# Security & JWT (Must be at least 32 characters / 256 bits)
JWT_SECRET=super_secret_jwt_key_that_is_at_least_32_characters_long_for_security_testing
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

# CORS Allowed Origins
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost

# Supabase Storage Integration
SUPABASE_URL=https://<supabase-project-ref>.supabase.co
SUPABASE_API_KEY=your-supabase-key
SUPABASE_STORAGE_BUCKET=academic-content
SUPABASE_PUBLIC_URL_PREFIX=https://<supabase-project-ref>.supabase.co/storage/v1/object/public

# Live Session Configuration
LIVE_SESSION_DAILY_API_KEY=test-daily-api-key
LIVE_SESSION_DAILY_DOMAIN=test-daily-domain
```

### 3.2 Frontend Configuration (`frontend/.env`)
Copy the frontend sample environment file:
```bash
cp frontend/.env.example frontend/.env
```

Edit `frontend/.env`:
```env
# Point to backend API during local development
VITE_API_BASE_URL=http://localhost:8080/api

# Supabase Storage prefix (matches backend configuration)
VITE_SUPABASE_STORAGE_PUBLIC_URL=https://<supabase-project-ref>.supabase.co/storage/v1/object/public
```

---

## 4. Running the Application Locally (Native Host)

Running directly on your host machine allows fast hot-reloading for frontend components and rapid debugging for the backend in your IDE.

### 4.1 Running the Backend (Spring Boot)
1. Open a terminal in `backend/`:
   ```bash
   cd backend
   ```
2. Build and run via Maven Wrapper:
   - On Linux/macOS:
     ```bash
     ./mvnw spring-boot:run
     ```
   - On Windows (PowerShell/CMD):
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
3. Flyway migrations (`V1` through `V6`) run automatically on startup.
4. Verify backend health:
   ```bash
   curl http://localhost:8080/actuator/health
   # Expected: {"status":"UP"}
   ```

### 4.2 Running the Frontend (Vite + React)
1. Open a separate terminal in `frontend/`:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Access the frontend at: `http://localhost:5173`
   - Requests to `/api/*` are handled by Axios pointing to `http://localhost:8080/api` with cookie transmission enabled (`withCredentials: true`).

---

## 5. Running the Application via Docker Compose

When Docker is available, you can run the entire stack with a single command.

### 5.1 Using Cloud Database (Default)
```bash
cp .env.example .env
# Edit .env with your Supabase credentials
docker compose up --build
```
Access points:
- Frontend: `http://localhost` (or port `80` / `3000`)
- Backend API: `http://localhost:8080`
- API via Frontend Reverse Proxy: `http://localhost/api/`

### 5.2 Using Local PostgreSQL Container
```bash
docker compose --profile local-db up --build
```
This boots an isolated PostgreSQL 16 container on port `5432` with a persistent named volume `pgdata`.

---

## 6. Running Test Suites

Continuous testing guarantees functional stability and security regression avoidance.

### 6.1 Backend Tests (JUnit 5 + Mockito + Testcontainers)
From the root directory or `backend/`:
```bash
cd backend
./mvnw test -Dtest="*Test,!FlywayMigrationSafetyTest"
```
On Windows:
```powershell
.\mvnw.cmd test -Dtest="*Test,!FlywayMigrationSafetyTest"
```
- Tests all 101 unit and integration test cases across auth, controllers, security filters, role guards, services, and live session orchestration.

### 6.2 Frontend Tests (Vitest + Testing Library)
From the root directory or `frontend/`:
```bash
cd frontend
npm test -- --run
```
- Runs all 31 unit and integration test suites covering role routing, auth context, layout rendering, and feature modules.

### 6.3 Frontend Production Build Check
Verify TypeScript type-checking and bundling:
```bash
cd frontend
npm run build
```
- Executes `tsc -b && vite build` ensuring 0 compilation and linting errors.

---

## 7. Seed Data & Test Role Accounts

The application includes pre-seeded roles and users via migration `V2__seed_academic_data.sql` and `V3__security_harden_remove_demo_users.sql`.

When creating test accounts for development:
- **Admin**: Create via the secure bootstrap administrative workflow or database seed.
- **Faculty**: Register via the frontend signup flow, selecting the **FACULTY** role. Accounts default to `PENDING` status and require approval by an Admin.
- **Student**: Register via the frontend signup flow, selecting the **STUDENT** role. Accounts default to `ACTIVE` or `PENDING` depending on institutional policy.

---

## 8. Common Developer Workflows

### 8.1 Adding a New Flyway Migration
1. Name the migration file following Flyway standards:
   `backend/src/main/resources/db/migration/V<NextVersion>__<descriptive_title>.sql`
2. Never modify an existing applied migration file (`V1` - `V6`).
3. Ensure all table and column names follow PostgreSQL snake_case conventions.
4. Maintain `ON DELETE CASCADE` or `ON DELETE SET NULL` constraints deliberately.

### 8.2 Refresh Token & Cookie Debugging
- Refresh tokens are delivered via HttpOnly, Secure, SameSite cookies.
- When testing on `localhost`, the cookie `Secure` flag should only be set in production (`isProduction()` check in `AuthService.java`).
- In browser DevTools -> Application -> Cookies -> `http://localhost:5173`, inspect the `refresh_token` cookie.
