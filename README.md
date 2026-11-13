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

This repository represents the full enterprise modernization of the legacy Virtual Classroom prototype. The legacy architecture suffered from raw JSP scriptlets, SQL string concatenation, plaintext credentials, and tight presentation coupling.

The modernization delivers a verified enterprise architectural standard:

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
├── .github/workflows/          # GitHub Actions CI/CD workflows (CI, Staging, Production)
├── backend/                    # Spring Boot 3.4.3 REST API (Java 21 LTS)
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
│   └── legacy/                 # Preserved legacy MySQL scripts and JSP prototype
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

## 🛠️ Tech Stack (Modern Platform)

| Layer | Technology | Description |
|---|---|---|
| **Frontend Framework** | React 18, TypeScript 5, Vite 6 | Type-safe Single Page Application |
| **Styling & Design** | Tailwind CSS 3.4, Lucide React Icons | Responsive modern design system |
| **State & API** | Axios, React Router 6, TanStack Query | Client-side routing and HTTP interceptors |
| **Backend Framework** | Spring Boot 3.4.3 (Java 21 LTS) | Modern RESTful microservices architecture |
| **Security** | Spring Security 6, JJWT (HMAC-SHA256), BCrypt | Stateless JWT Bearer auth, RBAC |
| **Database & ORM** | PostgreSQL 15+ (Supabase), Spring Data JPA | Relational persistence, connection pooling |
| **Database Migrations**| Flyway Migration Engine | Deterministic, version-controlled schema evolution |
| **Containerization** | Docker, Docker Compose | Multi-stage production container images |
| **CI/CD** | GitHub Actions | Automated linting, testing, and security gates |

---

## 🛠️ Quick Start & Local Setup

### Modern Prerequisites
- **Java**: OpenJDK 21 LTS or compatible (Tested on OpenJDK 21)
- **Node.js**: Node.js 20+ LTS & npm 10+
- **Database**: PostgreSQL 15+ or Supabase instance (H2 in-memory profile supported for offline testing)
- **Docker** *(Optional)*: Docker 24+ and Docker Compose v2+

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
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI / OpenAPI Docs: `http://localhost:8080/swagger-ui.html`
- Health Actuator: `http://localhost:8080/actuator/health`

### 3. Run Frontend Application
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:3000` or `http://localhost:5173` in your browser.

---

## 🐳 Docker Orchestration

The platform includes production-ready, multi-stage Dockerfiles and Docker Compose orchestration:

```bash
# Build and start all services (Backend, Frontend, and PostgreSQL)
docker compose -f infrastructure/docker/docker-compose.yml up --build -d

# Check service health and logs
docker compose -f infrastructure/docker/docker-compose.yml ps
docker compose -f infrastructure/docker/docker-compose.yml logs -f

# Shut down the stack
docker compose -f infrastructure/docker/docker-compose.yml down
```

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

## 🔄 CI/CD Automation

The repository utilizes automated GitHub Actions workflows under `.github/workflows/`:
- **`ci.yml`**: Automated continuous integration running backend unit/integration tests, frontend tests, and production build checks on pushes and pull requests targeting `main` and `develop`.
- **`staging-deployment.yml`**: Automated staging deployment workflow with explicit environment secret gates and post-deployment smoke testing.
- **`production-release.yml`**: Hardened production release workflow enforcing commit policies, preventing unauthorized or legacy baseline releases, and gating releases behind manual approvals.

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

In accordance with security hardening, production migrations (`V3`) strictly purged hardcoded credentials. For local development and demonstration testing, use the development seeding profile:

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

## 📜 Historical Architecture and Migration

### Origins & Software Engineering Case Study
The original Virtual Classroom application was designed as a university Software Engineering case study following Agile/Scrum methodology. The prototype explored digital lecture delivery, student/faculty registration approvals, and basic discussion forums.

### Legacy Technology Stack

| Layer | Technology (Legacy Prototype) |
|---|---|
| **Frontend** | HTML5, CSS3, Bootstrap 4, jQuery |
| **Backend** | Java Server Pages (JSP), Java Servlets |
| **Database** | MySQL (InnoDB) |
| **Styling** | SCSS, Font Awesome, Themify Icons, Flaticon |
| **Application Server** | Apache Tomcat |
| **Build Tool** | Apache Maven |
| **Version Control** | Git / Bitbucket |

> [!NOTE]
> **Historical Prerequisites Disclaimer:**
>
> The legacy prototype historically required **Java JDK 8+**, **Apache Tomcat 9+**, and **MySQL 8+**.
>
> **These technologies are NOT prerequisites for running the modern application.** The modern platform is entirely standalone, powered by **Java 21 LTS**, **Spring Boot 3.4**, and **PostgreSQL 15+**. Legacy SQL scripts and JSP prototypes have been archived in `database/legacy/` for historical reference and academic contrast.

### Legacy Database Schema (`project`)
The original MySQL prototype utilized 4 core tables:
- `admin`: Administrator credentials (SHA1-hashed).
- `student`: Student records with roll number, degree, course, year, and approval status.
- `faculty`: Faculty records with subject, course, year, and approval status.
- `enroll_for`: Valid degree-course combinations.

*(Archived SQL script: `database/legacy/project_DB.sql`)*

### Software Engineering Coursework Deliverables
All original Software Engineering course deliverables are preserved under `se-artifacts/`:

| Artifact | Location |
|---|---|
| Feasibility Report | `se-artifacts/feasibility-report/` |
| Software Requirements Specification (SRS) | `se-artifacts/srs/` |
| User Stories (Agile) | `se-artifacts/user-stories/` |
| Scrum / Standup Notes | `se-artifacts/scrum-notes/` |
| Test Cases | `se-artifacts/test-cases/` |
| Reference Material & Links | `se-artifacts/material/` |

### Historical SE Tooling & Quality Assurance
- **JIRA** — Sprint planning and issue tracking
- **Apache Maven** — Build automation
- **JUnit & Selenium** — Functional and UI testing
- **Cobertura & SonarQube** — Code quality and test coverage
- **Coverity** — Static security analysis
- **Jenkins** — Continuous integration
- **Bitbucket** — Version control and code reviews

### 📸 Legacy Prototype Screenshots

| Home / Landing | Student Login | Student Dashboard |
|---|---|---|
| ![Home](screenshots/01_home_landing.png) | ![Login](screenshots/02_student_login.png) | ![Student Dashboard](screenshots/04_student_dashboard.png) |

| Faculty Dashboard | Admin Dashboard | Q&A Forum |
|---|---|---|
| ![Faculty](screenshots/05_faculty_dashboard.png) | ![Admin](screenshots/06_admin_dashboard.png) | ![QnA](screenshots/07_qna_forum.png) |

| Student Registration | Study Materials |
|---|---|
| ![Register](screenshots/03_student_register.png) | ![Materials](screenshots/08_study_materials.png) |

---

## 🤝 Contributing Guidelines

1. **Branching Model**: All feature development branches must branch off `develop`. Direct pushes to `main` are restricted.
2. **Commit Convention**: Follow Conventional Commits (e.g., `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`).
3. **Pull Requests**: Submit PRs targeting `develop`. All CI pipeline tests and lint checks must pass prior to review.
4. **Code Quality**: Ensure all new backend code includes unit tests (JUnit 5 / Mockito) and frontend code passes Vitest tests and TypeScript type checking.

---

## 📄 License & Attribution

This project is open-source under the MIT License. Developed as a modern Software Engineering architectural transformation.

*Virtual Classroom — Empowering Education Through Technology*
