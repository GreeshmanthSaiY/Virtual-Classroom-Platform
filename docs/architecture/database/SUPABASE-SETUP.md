# Virtual Classroom Platform — Supabase Integration Foundation & Storage Guide

## 1. Supabase Architecture Overview

The Virtual Classroom Platform integrates with **Supabase** for two foundational cloud services:
1. **Managed PostgreSQL Database**: Hosted, highly-available PostgreSQL 14+ with connection pooling.
2. **Object Storage Engine**: S3-compatible blob storage with bucket policies for student and faculty academic assets.

> **CRITICAL SECURITY NOTE**: The platform retains **Spring Security + JWT** as its authoritative authentication, authorization, and business logic engine. Supabase Auth is **not** used. Supabase serves strictly as the underlying PostgreSQL and Object Storage infrastructure.

---

## 2. Supabase PostgreSQL Connection Topology

Supabase exposes two network entry points for database connectivity:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        SUPABASE CONNECTION MODES                             │
├──────────────────────────────┬───────────────────────────────────────────────┤
│ Mode 1: Direct Connection    │ Mode 2: Supavisor / PgBouncer Pooler          │
├──────────────────────────────┼───────────────────────────────────────────────┤
│ Host: db.<ref>.supabase.co   │ Host: aws-0-<region>.pooler.supabase.com      │
│ Port: 5432                   │ Port: 6543 (Transaction) / 5432 (Session)     │
│ Protocol: PostgreSQL Direct  │ Protocol: Transaction-level Connection Pooler │
│ Usage: Migrations (Flyway),  │ Usage: Runtime Backend App Traffic on Render  │
│        schema DDL, indexing  │        or containerized cloud environments    │
│ Concurrency: Direct conns    │ Concurrency: Hundreds of multiplexed clients  │
└──────────────────────────────┴───────────────────────────────────────────────┘
```

### 2.1 Connection String Formatting

#### Direct Connection (Port 5432 — Recommended for Flyway Migrations & Local Development):
```
jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require
```

#### Pooled Connection (Port 6543 — Recommended for Production App Runtime on Render):
```
jdbc:postgresql://aws-0-<REGION>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
```
*Note: `prepareThreshold=0` is required when using PgBouncer/Supavisor in transaction pooling mode to disable server-side prepared statements, avoiding `prepared statement "S_1" already exists` errors.*

---

## 3. Spring Boot Profile Configuration

The backend supports profile-driven database configuration via `application-<profile>.yml`:

### 3.1 Local Development Profile (`dev` / `default`)
Uses local PostgreSQL or Docker Compose:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/virtual_classroom}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:postgres}
```

### 3.2 Production Supabase Profile (`prod`)
Configured via `application-prod.yml` and environment variables supplied in Render:
```yaml
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
```

### 3.3 Render Environment Variables Specification

When deploying the backend web service on **Render**, configure the following environment variables in the Render Dashboard (**Environment Settings**):

| Environment Variable | Example / Value | Description |
|:---|:---|:---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates `application-prod.yml`. |
| `DATABASE_URL` | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0` | Pooled connection string pointing to Supavisor transaction pooler. |
| `DATABASE_USERNAME` | `postgres.your-project-ref` | Supabase pooler username (includes project reference tenant). |
| `DATABASE_PASSWORD` | `<SUPABASE_DB_PASSWORD>` | External database password (never committed to repository). |
| `JWT_SECRET` | `<HEX_OR_BASE64_64_CHAR_SECRET>` | Cryptographically random secret for HMAC-SHA256 JWT signing. |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | `https://virtualclassroom.onrender.com,http://localhost:3000` | Whitelist of frontend origins permitted to access REST API. |
| `SERVER_PORT` | `8080` | Port configured on Render container. |


---

## 4. Supabase Storage Architecture

Academic media and document files are partitioned into five dedicated storage buckets:

| Bucket Name | Visibility | Max File Size | Permitted MIME Types | Purpose | Access Control Model |
|:---|:---|:---|:---|:---|:---|
| `avatars` | **Public** | 5 MB | `image/jpeg`, `image/png`, `image/webp` | Profile photos for students, faculty, and admins. | Publicly readable; uploads restricted to owning user. |
| `materials` | **Private** | 50 MB | `application/pdf`, `application/vnd.ms-powerpoint`, `application/vnd.openxmlformats-officedocument.presentationml.presentation`, `application/zip` | Syllabi, lecture notes, textbook chapters. | Readable only by enrolled students and assigned faculty. |
| `lecture-attachments` | **Private** | 50 MB | `application/pdf`, `application/zip`, `text/plain` | Supplementary slide decks and code samples for recorded lectures. | Readable by enrolled students and faculty. |
| `assignment-attachments`| **Private** | 25 MB | `application/pdf`, `application/zip`, `text/markdown` | Instructor assignment prompts and starter templates. | Readable by enrolled students. |
| `submissions` | **Private** | 30 MB | `application/pdf`, `application/zip`, `application/gzip` | Student homework hand-ins and project reports. | Writable only by student; readable by student and instructor. |

---

## 5. Storage Security & Upload Strategy

### Recommended File Upload Pattern: Pre-Signed URLs
To avoid bottlenecking the Spring Boot application server with large multi-megabyte file uploads, the platform recommends **pre-signed URL generation**:

```
┌─────────┐                ┌─────────────────────┐                ┌──────────────────┐
│ Client  │                │ Spring Boot Backend │                │ Supabase Storage │
│ (React) │                │     (REST API)      │                │    (S3 / Blob)   │
└────┬────┘                └──────────┬──────────┘                └────────┬─────────┘
     │                                │                                    │
     │ 1. Request Upload Pre-Signed   │                                    │
     │    URL for file (e.g. sub.pdf) │                                    │
     │───────────────────────────────>│                                    │
     │                                │ 2. Verify RBAC & enrollment        │
     │                                │ 3. Generate pre-signed PUT URL     │
     │ 4. Return Pre-signed URL       │<───────────────────────────────────│
     │<───────────────────────────────│                                    │
     │                                                                     │
     │ 5. Direct PUT upload with file payload                              │
     │────────────────────────────────────────────────────────────────────>│
     │                                                                     │
     │ 6. Complete Submission: POST /api/v1/assignments/1/submit           │
     │    with { fileUrl: "materials/uuid-sub.pdf" }                       │
     │───────────────────────────────>│                                    │
     │                                │ 7. Persist record in PostgreSQL    │
     │ 8. Submission Confirmed        │                                    │
     │<───────────────────────────────│                                    │
```

### Security Safeguards
1. **Never Commit Supabase Service Role Key**:
   - The Supabase `service_role` key bypasses all RLS policies and holds root permissions. It must **never** be included in frontend code, git commits, or public repository files.
2. **Path Namespace Isolation**:
   - Files are stored under tenant-safe paths:
     - `avatars/{userId}/avatar-{timestamp}.png`
     - `materials/{subjectId}/{materialId}-{filename}`
     - `submissions/{assignmentId}/{studentId}/{timestamp}-{filename}`
3. **MIME Type Validation**:
   - Both client and backend validate MIME types against an explicit whitelist before permitting upload generation.

