# Database Architecture & Migration Management

## Overview
The Virtual Classroom Platform uses **PostgreSQL 16+** (hosted on Supabase in production and staging, and available via Docker Compose for local development).

## Canonical Flyway Migrations
Per enterprise standards and modular monolith architecture, the **single canonical source** for all database migrations is managed by Flyway in the backend resource path:
```
backend/src/main/resources/db/migration/
  ├── V1__init_schema.sql         # 12 Core Tables, Indexes & Foreign Key Constraints
  └── V2__seed_academic_data.sql   # Reference Curriculum, Initial Programs & Demo Data
```

Flyway automatically discovers and applies these versioned migrations during Spring Boot application startup in production (`application.yml`) and local profiles (`application-local.yml`).

## Directory Organization
- `database/legacy/`: Contains original academic prototypes, JSP references, and initial MySQL schema (`project_DB.sql`) preserved for academic documentation integrity.
- `database/diagrams/`: Entity Relationship (ER) diagrams and relational schema documentation.
- `database/seed/`: Additional sample datasets and development fixture seeds.

## Local Execution
To execute migrations locally:
1. Start the local PostgreSQL service:
   ```bash
   cd infrastructure/docker
   docker compose up -d db
   ```
2. Start the backend with the local profile:
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```
Flyway will automatically validate and execute any pending migrations against `virtual_classroom`.
