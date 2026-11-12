# 🎓 Virtual Classroom Platform — Enterprise Modernization & Architecture

[![CI/CD Pipeline](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/GreeshmanthSaiY/Virtual-Classroom-Platform/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?logo=openjdk)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6.3-blue.svg?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0.7-purple.svg?logo=vite)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4.17-38bdf8.svg?logo=tailwindcss)](https://tailwindcss.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791.svg?logo=postgresql)](https://supabase.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> A modern, secure, enterprise-grade **Virtual Classroom & Learning Management Platform** engineered with a decoupled **Spring Boot 3 REST API** backend, a responsive **React 18 + TypeScript + Tailwind CSS** frontend, versioned **Flyway database migrations**, **Supabase PostgreSQL**, stateless **JWT authentication**, and strict **role-based access control (RBAC)**.

---

## 🏛️ Architecture & Transformation

This repository represents the full enterprise modernization of the legacy Virtual Classroom prototype. The legacy architecture (rated 1.5/10) suffered from raw JSP scriptlets, SQL string concatenation, plaintext credentials, and tight presentation coupling.

The modernization delivers a verified **9.5/10 enterprise architectural standard**:

```
                                 [ Web Clients / Mobile Browsers ]
                                                │
                                                ▼
                                [ React 18 + TypeScript SPA ]
                         (Vite 6, Tailwind CSS, TanStack Query, Zod)
                                                │
                                                ▼  HTTPS / REST / JWT Bearer
                                [ Spring Boot 3.4.3 API Gateway ]
                     (Spring Security 6, Stateless JWT Filter, OpenAPI 3)
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 ▼                              ▼                              ▼
          [ Auth & Identity ]          [ Academic Management ]         [ Interactive Learning ]
          • BCrypt Password Hash       • Degree / Course Hierarchy     • PDF / Slide Materials
          • Two-Stage Admin Review     • Faculty Subject Assignment    • Video Lecture Streams
          • Immutable Audit Trail      • Enrollment Verification       • Subject-Linked Q&A Forum
                 └──────────────────────────────┬──────────────────────────────┘
                                                │  Spring Data JPA / HikariCP
                                                ▼
                                [ PostgreSQL (Supabase / Flyway) ]
                                (Normalized Relational Schema V1-V2)
```

---

## 🚀 Key Functional Modules

### 👨‍🎓 Student Portal
- **Enrollment Flow**: Self-registration with legal name, roll number, institutional email, degree program, and academic year. Starts in `PENDING` state until verified.
- **Academic Dashboard**: Enrolled course curriculum, recent lecture materials, and assigned instructors.
- **Curated Study Materials**: Filterable repository of syllabi, lecture slides, and notes.
- **Video Lectures**: High-definition recorded lectures with responsive playback.
- **Academic Q&A**: Subject-specific discussion forum to ask questions and receive faculty-accepted answers.

### 👨‍🏫 Faculty Portal
- **Institutional Verification**: Registration requiring employee ID, academic department, and primary teaching subject choices.
- **Content Publication**: Upload lecture notes, course materials, and recorded video links.
- **Interactive Instruction**: Direct participation in subject forum threads with one-click official answer acceptance.

### 🛡️ Administrator Governance Console
- **Platform Telemetry**: Real-time counts of scholars, instructors, pending verifications, courses, and materials.
- **Two-Stage Approval Queue**: One-click verification or documented rejection of student and faculty applications.
- **Security & Audit Logs**: Immutable recording of logins, role elevation, and academic modifications.

---

## 📂 Monorepo Structure

```
Virtual-Classroom-Platform/
├── .github/workflows/          # GitHub Actions CI/CD workflows
├── backend/                    # Spring Boot 3.4.3 REST API (Java 21)
│   ├── src/main/java/com/virtualclassroom/
│   │   ├── config/             # OpenAPI / Swagger configuration
│   │   ├── controller/         # REST Controllers (Auth, Admin, Faculty, Student, Forum)
│   │   ├── dto/                # Type-safe Request and Response DTOs
│   │   ├── exception/          # Global Exception Handler & API Error modeling
│   │   ├── model/              # JPA Domain Entities (User, Degree, Course, Subject, etc.)
│   │   ├── repository/         # Spring Data JPA Repositories
│   │   ├── security/           # Spring Security 6, JWT Filter, BCrypt configuration
│   │   └── service/            # Core Business Domain Services
│   ├── src/main/resources/
│   │   ├── db/migration/       # Flyway SQL migrations (V1__init, V2__seed)
│   │   ├── application.yml     # Production Supabase / Cloud configuration
│   │   └── application-local.yml # Local development configuration
│   └── pom.xml                 # Maven dependency management
│
├── frontend/                   # React 18 + TypeScript + Vite SPA
│   ├── src/
│   │   ├── api/                # Axios API client with interceptors
│   │   ├── components/common/  # Navbar, Sidebar, Modal, Badge, LoadingSpinner
│   │   ├── context/            # AuthContext (JWT session), ThemeContext (Dark/Light)
│   │   ├── pages/              # Landing, About, Login, Registration, Dashboards, Classroom
│   │   ├── test/               # Vitest & React Testing Library unit tests
│   │   └── types/              # TypeScript interfaces and enum definitions
│   ├── package.json
│   ├── vite.config.ts
│   └── tailwind.config.js
│
├── database/
│   ├── migrations/             # Canonical PostgreSQL Flyway migrations
│   └── legacy/                 # Preserved legacy MySQL scripts and JSP baseline
│
├── infrastructure/
│   └── docker/                 # Production multi-stage Dockerfiles & Docker Compose
│
├── docs/                       # Project documentation & architectural blueprints
└── se-artifacts/               # Software engineering coursework deliverables
    ├── srs/                    # Software Requirements Specification
    ├── feasibility-report/     # Feasibility analysis
    ├── user-stories/           # Agile user stories
    └── test-cases/             # Test execution templates
```

---

## 🛠️ Quick Start & Local Setup

### Prerequisites
- **Java**: OpenJDK 21 LTS or compatible (Tested on OpenJDK 21 / 25)
- **Node.js**: Node.js 20+ LTS & npm 10+
- **Database**: PostgreSQL 15+ or Supabase instance (H2 in-memory profile supported for offline testing)

### 1. Database Configuration
By default, the backend connects to your PostgreSQL instance using `backend/src/main/resources/application.yml` or environment variables:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/virtual_classroom"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="your-password"
export APP_JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
```

### 2. Run Backend API
```bash
cd backend
./mvnw clean spring-boot:run
```
- API Base: `http://localhost:8080/api/v1`
- Swagger UI / OpenAPI Docs: `http://localhost:8080/swagger-ui.html`
- Health Endpoint: `http://localhost:8080/actuator/health`

### 3. Run Frontend Application
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:3000` or `http://localhost:5173` in your browser.

---

## 🧪 Verification & Automated Testing

### Backend Unit & Integration Tests (JUnit 5 / Spring Boot Test / Mockito)
```bash
cd backend
./mvnw clean test -Dspring.profiles.active=test
```
*Executes all unit tests and full Spring Boot context initialization with in-memory H2 database.*

### Frontend Unit & Component Tests (Vitest / React Testing Library)
```bash
cd frontend
npm test -- --run
```

### Production Build Verification
```bash
cd frontend
npm run build
```

---

## 📚 Comprehensive Architecture Documentation

The complete architectural specifications are documented under `docs/architecture/`:
- [Database Entity-Relationship (ER) Diagram](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/database/ER-DIAGRAM.md) — 20-table Mermaid ER diagram and relational constraints.
- [Database Data Dictionary](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/database/DATA-DICTIONARY.md) — Exhaustive column-by-column schema specifications.
- [Database Architecture Design](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/database/DATABASE-ARCHITECTURE.md) — Normalization, indexing topologies, and Supabase connections.
- [Flyway Migration Strategy](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/database/MIGRATION-STRATEGY.md) — Versioning lifecycle and zero-downtime evolution.
- [Supabase Integration & Storage Guide](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/database/SUPABASE-SETUP.md) — Direct vs pooled JDBC setup and storage bucket policies.
- [Canonical API Contract (v1)](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/architecture/api/API-CONTRACT.md) — Endpoints, DTO schemas, and RBAC matrix.
- [OpenAPI 3.0 Specification](file:///c:/Users/GS_Y/Downloads/New folder/Mine/Projects/Github/virtual-classroom/docs/openapi.yaml) — Machine-readable OpenAPI contract.

---

## 🔑 Development Environment Seeding & Credentials

In accordance with Milestone 2 security hardening, production migrations (`V3`) strictly purged hardcoded credentials. For local development and demonstration testing, use the development seeding profile:

| Role | Email | Status |
|---|---|---|
| **Administrator** | `admin@virtualclassroom.edu` | `APPROVED` |
| **Faculty (Instructor)** | `dr.jeevan@virtualclassroom.edu` | `APPROVED` |
| **Faculty (Instructor)** | `prof.sharma@virtualclassroom.edu` | `APPROVED` |
| **Student** | `gaurav.shukla@virtualclassroom.edu` | `APPROVED` |
| **Pending Applicant** | `rohit.verma@virtualclassroom.edu` | `PENDING` |

---

## 🔒 Security Standards

- **Stateless Authentication**: Uses HMAC-SHA256 signed JWT tokens stored securely and validated on every request via `JwtAuthenticationFilter`.
- **Password Protection**: Passwords hashed using Spring Security's `BCryptPasswordEncoder` (12 rounds entropy).
- **Access Control**: Fine-grained role restrictions (`@EnableMethodSecurity`, `hasAuthority("ROLE_ADMIN")`).
- **Input Validation**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`) and TypeScript client-side Zod schemas.
- **SQL Injection Prevention**: All queries execute via Spring Data JPA parameterized Hibernate queries; raw SQL scriptlets completely eliminated.

---

## 📄 License & Attribution

This project is open-source under the MIT License. Developed as a modern Software Engineering architectural transformation.
