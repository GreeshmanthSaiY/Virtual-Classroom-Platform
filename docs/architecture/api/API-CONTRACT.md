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

> **Client Security Note**: For browser clients, the rotated `refreshToken` is delivered exclusively via an `HttpOnly`, `SameSite=Strict`, `Secure` (in production) cookie, and is omitted (`null`) from the JSON payload to prevent XSS script access and client storage leakage. Native/mobile applications provide the header `X-Token-Transport: payload` or `X-Client-Type: mobile` to receive the token in the JSON body.

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/auth/login` | Public | `LoginRequest` | `AuthResponse` | Authenticates user; returns JWT access token, sets HttpOnly cookie (or returns payload token for mobile). |
| `POST` | `/api/v1/auth/register/student` | Public | `RegisterStudentRequest`| `UserDto` | Registers student in `PENDING` review state. |
| `POST` | `/api/v1/auth/register/faculty` | Public | `RegisterFacultyRequest`| `UserDto` | Registers faculty in `PENDING` review state. |
| `POST` | `/api/v1/auth/refresh` | Public | `RefreshTokenRequest` (or cookie) | `AuthResponse` | Rotates refresh token (SHA-256 hashed) and issues fresh access token. |
| `POST` | `/api/v1/auth/logout` | Public / Authenticated | `RefreshTokenRequest` (or cookie) | `200 OK` | Revokes refresh token session and clears HttpOnly cookie. |
| `GET`  | `/api/v1/auth/me` | Authenticated | None | `UserDto` | Returns authenticated user principal profile. |
| `POST` | `/api/v1/auth/forgot-password` | Public | `ForgotPasswordRequest` | `ApiResponse<String>` | Dispatches time-limited (15m) single-use password recovery token with enumeration defense. |
| `POST` | `/api/v1/auth/reset-password` | Public | `ResetPasswordRequest` | `ApiResponse<Void>` | Validates token, updates password, and terminates all active sessions across devices. |
| `POST` | `/api/v1/auth/change-password` | Authenticated | `ChangePasswordRequest` | `ApiResponse<Void>` | Verifies current password, changes password, and invalidates other sessions. |

### 2.2 User Profiles (`/api/v1/users`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/users/profile` | Authenticated | None | `UserDto` | Fetches active profile (student roll/course or faculty dept/designation). |
| `PUT` | `/api/v1/users/profile` | Authenticated | `UpdateProfileRequest` | `UserDto` | Updates avatar URL, phone number, bio, or faculty office hours. |

### 2.3 Academic Catalog (`/api/v1/courses`, `/api/v1/subjects`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET` | `/api/v1/courses/degrees` | Public / Authenticated | None | `List<DegreeDto>` | Lists all academic degrees with nested course curricula. |
| `GET` | `/api/v1/courses/by-degree/{degreeId}` | Public / Authenticated | None | `List<CourseDto>` | Lists degree programs offered under an academic degree. |
| `GET` | `/api/v1/courses/{courseId}/subjects` | Public / Authenticated | None | `List<SubjectDto>` | Returns subjects belonging to a course, optionally filtered by year level. |
| `GET` | `/api/v1/courses/subjects` | Public / Authenticated | None | `List<SubjectDto>` | Returns all system subjects. |
| `PATCH`| `/api/v1/courses/{courseId}/archive` | Admin | None | `CourseDto` | Sets `archived = true` on course. |
| `PATCH`| `/api/v1/courses/{courseId}/unarchive` | Admin | None | `CourseDto` | Restores course from archived status. |

### 2.4 Subject Enrollment (`/api/v1/enrollments`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/enrollments` | Student / Admin | `subjectId`, `term` (param) | `StudentEnrollmentDto` | Enrolls student into subject matching degree & year. |
| `GET`  | `/api/v1/enrollments/my-courses` | Student / Admin | None | `List<StudentEnrollmentDto>` | Lists subjects enrolled by student (alias: `/my-subjects`). |
| `GET`  | `/api/v1/enrollments/subject/{subjectId}` | Faculty / Admin | None | `List<StudentEnrollmentDto>` | Returns class roster for an academic subject. |

### 2.5 Coursework & Submissions (`/api/v1/assignments`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/assignments` | Faculty (Assigned) / Admin | `CreateAssignmentRequest` | `AssignmentDto` | Creates assignment with due date, rubrics, max points, and notifies enrolled students. |
| `GET`  | `/api/v1/assignments/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<AssignmentDto>` | Retrieves paged list of subject assignments. |
| `POST` | `/api/v1/assignments/submit` | Enrolled Student | `SubmitAssignmentRequest` | `AssignmentSubmissionDto` | Submits coursework solution (file URL / text; alias: `/{id}/submit`). |
| `GET`  | `/api/v1/assignments/{id}/submissions` | Assigned Faculty / Admin | None | `List<AssignmentSubmissionDto>` | Instructor view of all submissions. |
| `PUT`  | `/api/v1/assignments/submissions/{id}/grade` | Assigned Faculty / Admin | `GradeSubmissionRequest` | `AssignmentSubmissionDto` | Records grade points (bounded to maxPoints), feedback, and notifies student (supports PUT/POST). |
| `GET`  | `/api/v1/assignments/my-submissions` | Student | None | `List<AssignmentSubmissionDto>` | Portfolio of student's own submissions (alias: `/submissions/my`). |

### 2.6 Live Classroom Sessions (`/api/v1/sessions`)

> **Architectural Scope Note**: The platform provides **session scheduling, lifecycle state management, and virtual meeting room orchestration**. Video conferencing is mediated via external or embedded room bridges (e.g. Jitsi Meet room URLs); the platform does not run an internal WebRTC SFU media relay server.

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/sessions` | Assigned Faculty / Admin | `CreateLiveSessionRequest` | `LiveSessionDto` | Schedules live lecture meeting (Jitsi / WebRTC room link) and checks for time overlaps. |
| `GET`  | `/api/v1/sessions/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<LiveSessionDto>` | Paged schedule of upcoming and completed sessions. |
| `PUT/PATCH`| `/api/v1/sessions/{id}/status` | Host Faculty / Admin | `status` (param) | `LiveSessionDto` | Updates lifecycle state (`SCHEDULED` -> `LIVE`/`CANCELLED`; `LIVE` -> `COMPLETED`/`CANCELLED`). |

### 2.7 Examinations & Grading (`/api/v1/examinations`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/examinations` | Assigned Faculty / Admin | `CreateExaminationRequest` | `ExaminationDto` | Schedules formal midterm, final, or quiz exam. |
| `GET`  | `/api/v1/examinations/subject/{subjectId}` | Enrolled / Assigned / Admin | Paging | `PageResponse<ExaminationDto>` | Paged list of scheduled examinations. |
| `POST` | `/api/v1/examinations/grades` | Assigned Faculty / Admin | `RecordGradeRequest` | `ExaminationResultDto` | Records student marks (bounded to totalMarks), grade letter, remarks, and notifies student (alias: `/grade`). |
| `GET`  | `/api/v1/examinations/{id}/results` | Assigned Faculty / Admin | None | `List<ExaminationResultDto>` | Complete class grading ledger for an exam. |
| `GET`  | `/api/v1/examinations/my-results` | Student | None | `List<ExaminationResultDto>` | Student's certified academic transcript results (alias: `/results/my`). |

### 2.8 Administrative Controls (`/api/v1/admin`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET`  | `/api/v1/admin/metrics` | Admin | None | `PlatformMetricsDto` | Aggregated metrics (students, faculty, assignments, exams, live sessions, approvals, suspensions). |
| `GET`  | `/api/v1/admin/students/pending` | Admin | None | `List<UserDto>` | Lists pending student registrations. |
| `GET`  | `/api/v1/admin/faculty/pending` | Admin | None | `List<UserDto>` | Lists pending faculty registrations. |
| `GET`  | `/api/v1/admin/users` | Admin | Query params | `PageResponse<UserDto>` | Paginated users filtered by role. |
| `GET`  | `/api/v1/admin/users/search` | Admin | Query params (`q`, `role`, `status`) | `PageResponse<UserDto>` | Keyword search over email, full name, roll number, and employee ID. |
| `POST` | `/api/v1/admin/approvals` | Admin | `ApprovalRequest` | `200 OK` | Approves user account for login access and dispatches notification. |
| `POST` | `/api/v1/admin/rejections` | Admin | `RejectRequest` | `200 OK` | Rejects registration with documented reason and dispatches notification. |
| `POST` | `/api/v1/admin/users/{userId}/suspend` | Admin | `SuspendUserRequest` | `200 OK` | Suspends active account, revokes all active session tokens, and logs audit action. |
| `POST` | `/api/v1/admin/users/{userId}/reactivate` | Admin | None | `200 OK` | Restores suspended account to APPROVED status and notifies user. |
| `POST` | `/api/v1/admin/subjects/{subjectId}/assign-faculty` | Admin | `AssignFacultyRequest` | `200 OK` | Assigns faculty instructor to curriculum subject. |
| `GET`  | `/api/v1/admin/audit-logs` | Admin | Paging | `PageResponse<AuditLog>` | Compliance audit event ledger with actor IP, entity, and action. |

### 2.9 Student Dashboard & Classroom (`/api/v1/students`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET`  | `/api/v1/students/dashboard` | Student / Admin | None | `StudentDashboardDto` | Aggregated dashboard: degree course, enrolled subjects, upcoming sessions, pending assignments, recent grades, unread notification count. |
| `GET`  | `/api/v1/students/course` | Student / Admin | None | `CourseDto` | Enrolled degree and program details. |
| `GET`  | `/api/v1/students/subjects` | Student / Admin | None | `List<SubjectDto>` | Enrolled subjects matching current year level. |
| `GET`  | `/api/v1/students/materials` | Student / Admin | `subjectId` (opt) | `List<StudyMaterialDto>` | Enrolled study materials. |
| `GET`  | `/api/v1/students/lectures` | Student / Admin | `subjectId` (opt) | `List<VideoLectureDto>` | Enrolled recorded video lectures. |

### 2.10 Faculty Dashboard & Instructional Operations (`/api/v1/faculty`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET`  | `/api/v1/faculty/dashboard` | Faculty / Admin | None | `FacultyDashboardDto` | Aggregated dashboard: assigned subjects, upload counts, upcoming sessions, pending submissions awaiting grading, recent forum questions. |
| `GET`  | `/api/v1/faculty/subjects` | Faculty / Admin | None | `List<SubjectDto>` | List of curriculum subjects assigned to faculty. |
| `GET`  | `/api/v1/faculty/subjects/{subjectId}/students` | Assigned Faculty / Admin | None | `List<UserDto>` | Inspects enrolled student roster for an assigned subject. |
| `GET`  | `/api/v1/faculty/materials` | Faculty / Admin | `subjectId` (opt) | `List<StudyMaterialDto>` | Study materials uploaded by this faculty member. |
| `POST` | `/api/v1/faculty/materials` | Assigned Faculty / Admin | `CreateMaterialRequest` | `StudyMaterialDto` | Uploads and publishes study material. |
| `DELETE`|`/api/v1/faculty/materials/{id}`| Author / Admin | None | `200 OK` | Deletes study material. |
| `GET`  | `/api/v1/faculty/lectures` | Faculty / Admin | `subjectId` (opt) | `List<VideoLectureDto>` | Video lectures published by this faculty member. |
| `POST` | `/api/v1/faculty/lectures` | Assigned Faculty / Admin | `CreateLectureRequest` | `VideoLectureDto` | Publishes video lecture. |
| `DELETE`|`/api/v1/faculty/lectures/{id}` | Author / Admin | None | `200 OK` | Deletes video lecture. |

### 2.11 Discussion Threads & Q&A (`/api/v1/discussions`, `/api/v1/forum`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET`  | `/api/v1/discussions/questions` | Authenticated | `subjectId` (opt) | `List<QuestionDto>` | Lists discussion questions. |
| `GET`  | `/api/v1/discussions/search` | Authenticated | `q`, `subjectId`, Paging | `PageResponse<QuestionDto>` | Keyword search over question titles and bodies. |
| `GET`  | `/api/v1/discussions/questions/{id}` | Authenticated | None | `QuestionDto` | Thread details with all submitted answers. |
| `POST` | `/api/v1/discussions/questions` | Authenticated | `CreateQuestionRequest` | `QuestionDto` | Posts new question in a subject forum. |
| `PUT`  | `/api/v1/discussions/questions/{id}` | Author / Admin | `UpdateQuestionRequest` | `QuestionDto` | Updates question title and body. |
| `DELETE`|`/api/v1/discussions/questions/{id}`| Author / Admin | None | `200 OK` | Deletes question and associated answers. |
| `POST` | `/api/v1/discussions/questions/{id}/answers` | Authenticated | `CreateAnswerRequest` | `AnswerDto` | Submits answer to a question and notifies question author. |
| `PUT`  | `/api/v1/discussions/answers/{id}` | Author / Admin | `UpdateAnswerRequest` | `AnswerDto` | Updates answer text. |
| `DELETE`|`/api/v1/discussions/answers/{id}` | Author / Admin | None | `200 OK` | Deletes answer. |
| `PATCH`| `/api/v1/discussions/questions/{id}/resolve` | Author / Admin | None | `200 OK` | Marks question as resolved. |
| `PATCH`| `/api/v1/discussions/answers/{id}/accept` | Question Author / Admin | None | `200 OK` | Marks answer as accepted solution. |
| `PATCH`| `/api/v1/discussions/answers/{id}/endorse` | Assigned Faculty / Admin | None | `AnswerDto` | Endorses high-quality answer and notifies author. |

### 2.12 Cloud & Local File Storage (`/api/v1/storage`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `POST` | `/api/v1/storage/upload` | Authenticated | `multipart/form-data` | `FileUploadResponse` | Uploads document/image/video with MIME verification, size limit enforcement, and safe sanitization. |
| `POST` | `/api/v1/storage/signed-upload-url` | Authenticated | `SignedUploadUrlRequest` | `SignedUploadUrlResponse` | Generates signed URL for direct client-side storage upload (Supabase/local). |
| `GET`  | `/api/v1/storage/signed-download-url` | Authenticated | `path`, `expiresIn` | `ApiResponse<String>` | Generates time-limited signed download URL. |
| `GET`  | `/api/v1/storage/files/**` | Public / Authenticated | None | Binary Stream | Safe streaming of local storage assets with directory traversal protection. |
| `DELETE`|`/api/v1/storage/files` | Faculty / Admin | `path` | `200 OK` | Deletes file from storage bucket or local directory. |

### 2.13 In-App Notifications (`/api/v1/notifications`)

| Method | Endpoint | Access | Request Body | Response Body | Description |
|:---|:---|:---|:---|:---|:---|
| `GET`  | `/api/v1/notifications` | Authenticated | None | `List<NotificationDto>` | Lists user notifications ordered newest first. |
| `GET`  | `/api/v1/notifications/unread-count` | Authenticated | None | `ApiResponse<Long>` | Count of unread notifications. |
| `PATCH`| `/api/v1/notifications/{id}/read` | Authenticated | None | `200 OK` | Marks single notification as read. |
| `PATCH`| `/api/v1/notifications/read-all` | Authenticated | None | `200 OK` | Marks all notifications as read. |

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

