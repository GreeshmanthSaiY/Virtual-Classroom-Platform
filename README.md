# 🎓 Virtual Classroom — A Software Engineering Case Study

> A full-stack web application that enables interactive online learning for students and faculty. Built as a Software Engineering course case study using **JSP**, **Java Servlets**, **MySQL**, and **Bootstrap** — following Agile/Scrum methodology.

---

## 📸 Screenshots

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

## 🚀 Features

### 👨‍🎓 Student Portal
- Register with personal and academic details (degree, course, year)
- Login with roll number and password (SHA1-hashed)
- Session-based access control — redirected to login if not authenticated
- Access video lectures and study materials uploaded by faculty
- Participate in the Q&A discussion forum
- Admin approval required before first login

### 👨‍🏫 Faculty Portal
- Register with subject and course details
- Upload study materials for specific courses and years
- Manage and respond to student Q&A
- Admin approval required before first login

### 🔐 Admin Panel
- Secure admin login
- Approve or reject student and faculty registration requests
- View all pending and approved users
- Full control over platform access

---



## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, CSS3, Bootstrap 4, jQuery |
| **Backend** | Java Server Pages (JSP), Java Servlets |
| **Database** | MySQL (InnoDB) |
| **Styling** | SCSS, Font Awesome, Themify Icons, Flaticon |
| **Server** | Apache Tomcat |
| **Build Tool** | Apache Maven |
| **Version Control** | Git / Bitbucket |

---

## 🗄️ Database Schema

The database (`project`) contains the following tables:

| Table | Description |
|---|---|
| `admin` | Admin credentials (SHA1-hashed password) |
| `student` | Student records with roll number, degree, course, year, approval status |
| `faculty` | Faculty records with subject, course, year, approval status |
| `enroll_for` | Valid degree-course combinations |

### Setup

```sql
-- Run the SQL script to create and seed the database
mysql -u root -p < database/project_DB.sql
```

---

### Prerequisites

- Java JDK 8+
- Apache Tomcat 9+
- MySQL 8+
- Apache Maven


## 📋 Software Engineering Artifacts

This project was developed as part of a Software Engineering course following Agile methodology. All SE deliverables are in the `se-artifacts/` folder:

| Artifact | Location |
|---|---|
| Feasibility Report | `se-artifacts/feasibility-report/` |
| Software Requirements Specification (SRS) | `se-artifacts/srs/` |
| User Stories (Agile) | `se-artifacts/user-stories/` |
| Scrum / Standup Notes | `se-artifacts/scrum-notes/` |
| Test Cases | `se-artifacts/test-cases/` |
| Reference Material & Links | `se-artifacts/material/` |

---

## 🔧 SE Tools Used

- **JIRA** — Sprint planning and issue tracking
- **Apache Maven** — Build automation
- **JUnit** — Unit testing
- **Selenium** — UI/functional testing
- **Cobertura** — Code coverage
- **Coverity** — Static code analysis
- **SonarQube** — Code quality
- **Jenkins** — CI/CD pipeline
- **Bitbucket** — Version control & code review

---

## 🚀 Future Improvements

- Add real-time video conferencing (WebRTC integration)
- Implement live chat during lectures
- Add assignment submission and grading system
- Build a notification system for approvals and new materials
- Make fully responsive for mobile devices
- Deploy to cloud (AWS / Azure / GCP)
---

*Virtual Classroom — Empowering Education Through Technology*
