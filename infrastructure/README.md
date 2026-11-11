# Infrastructure & Local Development Guide

## Overview
This directory contains containerization and local development setup configurations for the Virtual Classroom Platform.

## Directory Structure
- `docker/`: Dockerfiles and `docker-compose.yml` for local multi-service container orchestration.
- `scripts/`: Cross-platform helper scripts to boot local services and run verification suites.

## Prerequisites
- **Java**: OpenJDK 21 LTS (`JAVA_HOME` set)
- **Node.js**: v20 LTS (`.nvmrc` included)
- **Docker & Docker Compose** (Optional for containerized mode)
- **PostgreSQL 16+** (If running natively without Docker)

---

## Running with Docker Compose (Recommended for Full Stack)
1. Ensure Docker Desktop is active.
2. From the project root, navigate to the docker folder:
   ```bash
   cd infrastructure/docker
   docker compose up --build
   ```
3. Services will be available at:
   - **Frontend**: http://localhost:3000
   - **Backend API**: http://localhost:8080/api/v1
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **PostgreSQL**: localhost:5432

---

## Running Natively (Fastest for Local Development)

### 1. Database
Either start only the database container:
```bash
cd infrastructure/docker
docker compose up -d db
```
Or use a local PostgreSQL instance with database `virtual_classroom` owned by user `postgres`.

### 2. Backend (Spring Boot 3.4)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
*(On Windows: `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`)*

### 3. Frontend (React 18 + Vite)
```bash
cd frontend
npm run dev
```
Access the application at `http://localhost:5173`.
