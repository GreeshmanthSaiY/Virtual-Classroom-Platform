# Virtual Classroom Platform — Canonical API Contract (v1)

## 1. Global Conventions

### 1.1 Base URL
All API endpoints are versioned and mounted under:
```
/api/v1
```

### 1.2 Authentication & Authorization Headers
Protected endpoints require an `Authorization` header containing a valid Bearer token:
```http
Authorization: Bearer <ACCESS_TOKEN>
```

### 1.3 Standard Error Response Schema (`ApiErrorResponse`)
All client or server errors return a standard JSON structure with HTTP status codes conforming to RFC 7807:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed description of error cause",
  "timestamp": "2026-11-12T17:45:00.000Z",
  "path": "/api/v1/assignments",
  "validationErrors": {
    "title": "Title cannot exceed 200 characters",
    "dueDate": "Due date must be in the future"
  }
}
```

### 1.4 Standard Paginated Response Schema (`PageResponse<T>`)
Endpoints that return paged collections accept `page` (0-indexed, default 0), `size` (default 20), and `sort` parameters and return:
```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 48,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

---

## 2. API Endpoints by Domain

### 2.1 Authentication & Token Management (`/api/v1/auth`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/auth/login` | Public | `LoginRequest` | `AuthResponse` | Authenticates user; returns JWT access token and rotated refresh token. |
| `POST` | `/api/v1/auth/register/student` | Public | `RegisterStudentRequest`| `UserDto` | Registers student in `PENDING` review state. |
| `POST` | `/api/v1/auth/register/faculty` | Public | `RegisterFacultyRequest`| `UserDto` | Registers faculty in `PENDING` review state. |
| `POST` | `/api/v1/auth/refresh` | Public | `RefreshTokenRequest` | `AuthResponse` | Rotates refresh token and issues fresh access token. |
| `POST` | `/api/v1/auth/logout` | Authenticated | `RefreshTokenRequest` | `200 OK` | Revokes refresh token session. |
| `GET` | `/api/v1/auth/me` | Authenticated | None | `UserDto` | Returns authenticated user principal profile. |

### 2.2 User Profiles (`/api/v1/users`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/users/profile` | Authenticated | None | `UserDto` | Fetches active profile (student roll/course or faculty dept/designation). |
| `PUT` | `/api/v1/users/profile` | Authenticated | `UpdateProfileRequest` | `UserDto` | Updates avatar URL, phone number, bio, or faculty office hours. |

### 2.3 Academic Catalog (`/api/v1/courses`, `/api/v1/subjects`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/courses` | Authenticated | None | `List<CourseDto>` | Lists all academic degree programs. |
| `GET` | `/api/v1/courses/{id}` | Authenticated | None | `CourseDto` | Returns course details with subject count. |
| `POST`| `/api/v1/courses` | Admin | `CourseDto` | `CourseDto` | Creates new degree curriculum. |
| `GET` | `/api/v1/subjects` | Authenticated | Query params | `List<SubjectDto>` | Lists curriculum subjects (filter by course or year). |
| `GET` | `/api/v1/subjects/{id}` | Authenticated | None | `SubjectDto` | Returns syllabus, semester, credits, description. |
| `POST`| `/api/v1/subjects` | Admin | `SubjectDto` | `SubjectDto` | Creates new subject module. |

### 2.4 Subject Enrollment (`/api/v1/enrollments`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST`| `/api/v1/enrollments` | Student / Admin | `EnrollRequest` | `StudentEnrollmentDto` | Enrolls student into subject matching degree & year. |
| `GET` | `/api/v1/enrollments/student/{studentId}` | Self / Admin | None | `List<StudentEnrollmentDto>` | Lists subjects enrolled by student. |
| `GET` | `/api/v1/enrollments/subject/{subjectId}` | Faculty / Admin | None | `List<StudentEnrollmentDto>` | Returns class roster for an academic subject. |

### 2.5 Coursework & Submissions (`/api/v1/assignments`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST`| `/api/v1/assignments` | Faculty (Assigned) / Admin | `CreateAssignmentRequest` | `AssignmentDto` | Creates assignment with due date, rubrics, max points. |
| `GET` | `/api/v1/assignments/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<AssignmentDto>` | Retrieves paged list of subject assignments. |
| `GET` | `/api/v1/assignments/{id}` | Enrolled / Assigned / Admin | None | `AssignmentDto` | Retrieves assignment details. |
| `POST`| `/api/v1/assignments/{id}/submit` | Enrolled Student | `SubmitAssignmentRequest` | `AssignmentSubmissionDto` | Submits coursework solution (file URL / text). |
| `GET` | `/api/v1/assignments/{id}/submissions` | Assigned Faculty / Admin | None | `List<AssignmentSubmissionDto>` | Instructor view of all submissions. |
| `POST`| `/api/v1/assignments/submissions/{id}/grade` | Assigned Faculty / Admin | `GradeSubmissionRequest` | `AssignmentSubmissionDto` | Records grade points and feedback. |
| `GET` | `/api/v1/assignments/submissions/my` | Student | None | `List<AssignmentSubmissionDto>` | Portfolio of student's own submissions. |

### 2.6 Live Classroom Sessions (`/api/v1/sessions`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST`| `/api/v1/sessions` | Assigned Faculty / Admin | `CreateLiveSessionRequest` | `LiveSessionDto` | Schedules live lecture meeting (Jitsi / WebRTC). |
| `GET` | `/api/v1/sessions/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<LiveSessionDto>` | Paged schedule of upcoming and recorded sessions. |
| `GET` | `/api/v1/sessions/{id}` | Enrolled / Assigned / Admin | None | `LiveSessionDto` | Retrieves meeting details and connection link. |
| `PATCH`|`/api/v1/sessions/{id}/status` | Host Faculty / Admin | `{ "status": "LIVE" }` | `LiveSessionDto` | Updates lifecycle state (`LIVE`, `COMPLETED`, `CANCELLED`). |

### 2.7 Examinations & Grading (`/api/v1/examinations`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST`| `/api/v1/examinations` | Assigned Faculty / Admin | `CreateExaminationRequest` | `ExaminationDto` | Schedules formal midterm, final, or quiz exam. |
| `GET` | `/api/v1/examinations/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<ExaminationDto>` | Paged list of scheduled examinations. |
| `GET` | `/api/v1/examinations/{id}` | Enrolled / Assigned / Admin | None | `ExaminationDto` | Retrieves exam duration, instructions, max marks. |
| `POST`| `/api/v1/examinations/grade` | Assigned Faculty / Admin | `RecordGradeRequest` | `ExaminationResultDto` | Records student marks, grade letter, and remarks. |
| `GET` | `/api/v1/examinations/{id}/results` | Assigned Faculty / Admin | None | `List<ExaminationResultDto>` | Complete class grading ledger for an exam. |
| `GET` | `/api/v1/examinations/results/my` | Student | None | `List<ExaminationResultDto>` | Student's certified academic transcript results. |

### 2.8 Administrative Controls (`/api/v1/admin`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/admin/users/pending` | Admin | None | `List<UserDto>` | Lists pending student and faculty registrations. |
| `POST`| `/api/v1/admin/users/{id}/approve` | Admin | None | `UserDto` | Approves user account for login access. |
| `POST`| `/api/v1/admin/users/{id}/reject` | Admin | `{ "reason": "..." }` | `UserDto` | Rejects registration with documented reason. |
| `POST`| `/api/v1/admin/users/{id}/suspend` | Admin | None | `UserDto` | Suspends active account. |
| `POST`| `/api/v1/admin/users/{id}/reactivate` | Admin | None | `UserDto` | Restores suspended account to active status. |
| `GET` | `/api/v1/admin/audit-logs` | Admin | Paging / Filter | `PageResponse<AuditLogDto>` | Compliance audit event ledger. |

---

## 3. Role-Based Access Control (RBAC) Matrix

| Domain Resource | Guest | Student (Enrolled) | Student (Unenrolled) | Faculty (Assigned) | Faculty (Unassigned) | Admin |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| User Registration / Login | `CREATE` | `READ` | `READ` | `READ` | `READ` | `ALL` |
| View Subject Syllabus | - | `READ` | `READ` | `READ` | `READ` | `ALL` |
| Self-Enroll in Eligible Subject | - | `CREATE` | `CREATE` | - | - | `ALL` |
| View Course Study Materials | - | `READ` | - | `READ` | - | `ALL` |
| Upload Course Study Materials | - | - | - | `CREATE` | - | `ALL` |
| View Recorded Video Lectures | - | `READ` | - | `READ` | - | `ALL` |
| Join Live Classroom Session | - | `READ` | - | `HOST/EDIT` | - | `ALL` |
| View Assignments | - | `READ` | - | `READ` | - | `ALL` |
| Submit Assignment Solution | - | `CREATE` | - | - | - | - |
| Grade Assignment Submissions | - | - | - | `UPDATE` | - | `ALL` |
| View Examination Schedule | - | `READ` | - | `READ` | - | `ALL` |
| Certify Exam Results | - | - | - | `CREATE` | - | `ALL` |
| View Personal Grade Transcript | - | `READ` | - | - | - | `ALL` |
| Approve / Reject Users | - | - | - | - | - | `ALL` |
| View Audit Logs | - | - | - | - | - | `ALL` |
