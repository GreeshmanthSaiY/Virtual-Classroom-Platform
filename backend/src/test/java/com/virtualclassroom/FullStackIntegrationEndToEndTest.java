package com.virtualclassroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 6 — Full-Stack End-to-End Integration, Multi-Role Lifecycle & Security Gate
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FullStackIntegrationEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private FacultyProfileRepository facultyProfileRepository;

    @Autowired
    private DegreeRepository degreeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private FacultySubjectAssignmentRepository facultySubjectAssignmentRepository;

    @Autowired
    private StudentSubjectEnrollmentRepository enrollmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private ExaminationRepository examinationRepository;

    @Autowired
    private ExaminationResultRepository examinationResultRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Degree seededDegree;
    private Course seededCourse;
    private Subject seededSubject;
    private User seededAdmin;

    @BeforeAll
    void setupGlobalAcademicContext() {
        // Initialize reference degree, course, subject and administrator
        seededDegree = degreeRepository.save(Degree.builder()
                .code("BTECH-CS")
                .name("Bachelor of Technology in Computer Science")
                .durationYears(4)
                .build());

        seededCourse = courseRepository.save(Course.builder()
                .degree(seededDegree)
                .code("CSE-CORE")
                .name("Computer Science and Engineering")
                .archived(false)
                .build());

        seededSubject = subjectRepository.save(Subject.builder()
                .course(seededCourse)
                .code("CS301")
                .title("Advanced Data Structures & Algorithms")
                .yearLevel(3)
                .build());

        seededAdmin = userRepository.findByEmail("admin.root@virtualclassroom.edu").orElseGet(() -> {
            User admin = new User();
            admin.setEmail("admin.root@virtualclassroom.edu");
            admin.setPasswordHash(passwordEncoder.encode("AdminRoot@2025!"));
            admin.setFullName("Super Administrator");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setStatus(UserStatus.APPROVED);
            return userRepository.save(admin);
        });
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    // =========================================================================
    // SECTION 1: AUTHENTICATION, REGISTRATION & SESSION SECURITY
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Auth E2E: Complete registration, pending block, approval, cookie issuance, and refresh lifecycle")
    void test01_AuthenticationLifecycle_EndToEnd() throws Exception {
        // 1. Register a student
        RegisterStudentRequest studentReq = new RegisterStudentRequest();
        studentReq.setEmail("student.e2e@virtualclassroom.edu");
        studentReq.setPassword("StudentPass@2025!");
        studentReq.setFullName("Student E2E Tester");
        studentReq.setCourseId(seededCourse.getId());
        studentReq.setYear(3);
        studentReq.setRollNumber("2026-CS-001");

        mockMvc.perform(post("/api/v1/auth/register/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Pending student login attempt must be rejected with 403 Forbidden
        LoginRequest pendingLogin = new LoginRequest();
        pendingLogin.setEmail("student.e2e@virtualclassroom.edu");
        pendingLogin.setPassword("StudentPass@2025!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("awaiting Administrator approval")));

        // 3. Register a faculty member
        RegisterFacultyRequest facultyReq = new RegisterFacultyRequest();
        facultyReq.setEmail("faculty.e2e@virtualclassroom.edu");
        facultyReq.setPassword("FacultyPass@2025!");
        facultyReq.setFullName("Dr. Faculty E2E Tester");
        facultyReq.setEmployeeId("FAC-2026-001");
        facultyReq.setDepartment("Computer Science");
        facultyReq.setInitialSubjectIds(List.of(seededSubject.getId()));

        mockMvc.perform(post("/api/v1/auth/register/faculty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facultyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Admin logs in and retrieves pending registration lists
        String adminToken = obtainToken(seededAdmin.getEmail(), "AdminRoot@2025!");

        MvcResult pendingStudentsRes = mockMvc.perform(get("/api/v1/admin/students/pending")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pendingStudents = objectMapper.readTree(pendingStudentsRes.getResponse().getContentAsString()).path("data");
        assertTrue(pendingStudents.size() > 0, "Pending students list should contain registered student");

        Long studentId = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow().getId();
        Long facultyId = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow().getId();

        // 5. Admin approves both candidates
        ApprovalRequest approvalReq = new ApprovalRequest();
        approvalReq.setUserIds(List.of(studentId, facultyId));

        mockMvc.perform(post("/api/v1/admin/approvals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. Approved student now logs in successfully, receiving Bearer token and HttpOnly refresh cookie
        MvcResult approvedStudentLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingLogin)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andReturn();

        Cookie refreshCookie = approvedStudentLogin.getResponse().getCookie("refreshToken");
        assertNotNull(refreshCookie);
        String studentToken = objectMapper.readTree(approvedStudentLogin.getResponse().getContentAsString()).path("data").path("token").asText();

        // 7. Student accesses authenticated /auth/me profile
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student.e2e@virtualclassroom.edu"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 8. Silent refresh rotation using HttpOnly cookie
        Thread.sleep(1050);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        String rotatedAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).path("data").path("token").asText();
        assertNotNull(rotatedAccessToken);
        assertNotEquals(studentToken, rotatedAccessToken, "Access token should be regenerated on refresh");

        // 9. Logout terminates active session and clears refresh cookie
        Cookie rotatedRefreshCookie = refreshResult.getResponse().getCookie("refreshToken");
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(rotatedRefreshCookie)
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));

        // 10. Replay of revoked refresh token is strictly rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(rotatedRefreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // SECTION 2: ADMIN GOVERNANCE, SUSPENSION, REACTIVATION & ARCHIVAL
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Admin E2E: User search, suspension lifecycle, session termination, and course archival")
    void test02_AdminGovernanceLifecycle_EndToEnd() throws Exception {
        String adminToken = obtainToken(seededAdmin.getEmail(), "AdminRoot@2025!");

        // 1. Search users by keyword, role, and active status
        mockMvc.perform(get("/api/v1/admin/users/search")
                        .param("q", "Tester")
                        .param("role", "ROLE_STUDENT")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("student.e2e@virtualclassroom.edu"));

        Long studentId = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow().getId();

        // 2. Suspend student account
        SuspendUserRequest suspendReq = new SuspendUserRequest();
        suspendReq.setReason("Administrative academic misconduct investigation");

        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify status in repository
        assertEquals(UserStatus.SUSPENDED, userRepository.findById(studentId).orElseThrow().getStatus());

        // 3. Suspended account cannot log in
        LoginRequest suspendedLogin = new LoginRequest();
        suspendedLogin.setEmail("student.e2e@virtualclassroom.edu");
        suspendedLogin.setPassword("StudentPass@2025!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendedLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("suspended")));

        // 4. Reactivate account
        mockMvc.perform(post("/api/v1/admin/users/" + studentId + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertEquals(UserStatus.APPROVED, userRepository.findById(studentId).orElseThrow().getStatus());

        // 5. Course archival and unarchival
        mockMvc.perform(patch("/api/v1/courses/" + seededCourse.getId() + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(true));

        assertTrue(courseRepository.findById(seededCourse.getId()).orElseThrow().isArchived());

        mockMvc.perform(patch("/api/v1/courses/" + seededCourse.getId() + "/unarchive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archived").value(false));

        assertFalse(courseRepository.findById(seededCourse.getId()).orElseThrow().isArchived());

        // 6. Assign Faculty to Subject
        Long facultyId = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow().getId();
        AssignFacultyRequest assignReq = new AssignFacultyRequest(facultyId, "2026-ACTIVE");

        mockMvc.perform(post("/api/v1/admin/subjects/" + seededSubject.getId() + "/assign-faculty")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // SECTION 3: CROSS-ROLE ACADEMIC WORKFLOW (ASSIGNMENTS, EXAMS, SESSIONS)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Academic Lifecycle E2E: Assignment publication, student submission, faculty grading, and grade visibility")
    void test03_CrossRoleAcademicWorkflow_Assignment_Submission_Grading() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        // 1. Enroll student in subject
        enrollmentRepository.save(new StudentSubjectEnrollment(student, seededSubject, "2026-ACTIVE", EnrollmentStatus.ENROLLED));

        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");
        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");

        // 2. Faculty creates assignment
        CreateAssignmentRequest asgReq = new CreateAssignmentRequest();
        asgReq.setSubjectId(seededSubject.getId());
        asgReq.setTitle("Dynamic Programming Problem Set 1");
        asgReq.setDescription("Implement Bellman-Ford and Floyd-Warshall with path reconstruction.");
        asgReq.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));
        asgReq.setMaxPoints(100);

        MvcResult asgResult = mockMvc.perform(post("/api/v1/assignments")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(asgReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Dynamic Programming Problem Set 1"))
                .andReturn();

        Long assignmentId = objectMapper.readTree(asgResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Student views assignment in subject coursework
        mockMvc.perform(get("/api/v1/assignments/subject/" + seededSubject.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(assignmentId));

        // 4. Student submits solution
        SubmitAssignmentRequest submitReq = new SubmitAssignmentRequest();
        submitReq.setAssignmentId(assignmentId);
        submitReq.setSubmissionText("All edge cases handled including negative-weight cycle detection.");
        submitReq.setFileUrl("https://storage.supabase.co/academic-assets/submissions/dp_sol.py");

        MvcResult submitResult = mockMvc.perform(post("/api/v1/assignments/" + assignmentId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn();

        Long submissionId = objectMapper.readTree(submitResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 5. Faculty retrieves all submissions for the assignment
        mockMvc.perform(get("/api/v1/assignments/" + assignmentId + "/submissions")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(submissionId))
                .andExpect(jsonPath("$.data[0].studentName").value("Student E2E Tester"));

        // 6. Faculty grades the student submission
        GradeSubmissionRequest gradeReq = new GradeSubmissionRequest();
        gradeReq.setGradePoints(95);
        gradeReq.setFeedback("Superb algorithmic complexity and clean code structure.");

        mockMvc.perform(post("/api/v1/assignments/submissions/" + submissionId + "/grade")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradePoints").value(95))
                .andExpect(jsonPath("$.data.feedback").value("Superb algorithmic complexity and clean code structure."));

        // 7. Student retrieves verified submission record and sees grade points
        mockMvc.perform(get("/api/v1/assignments/my-submissions")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(submissionId))
                .andExpect(jsonPath("$.data[0].gradePoints").value(95));
    }

    @Test
    @Order(4)
    @DisplayName("Academic Lifecycle E2E: Examination scheduling, grade letter recording, and student result inspection")
    void test04_CrossRoleAcademicWorkflow_Examination_Grades() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");
        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");

        // 1. Faculty schedules examination
        CreateExaminationRequest examReq = new CreateExaminationRequest();
        examReq.setSubjectId(seededSubject.getId());
        examReq.setTitle("Midterm Examination - Algorithms & Graph Theory");
        examReq.setExamType(ExamType.MIDTERM);
        examReq.setTotalMarks(100);
        examReq.setDurationMinutes(120);
        examReq.setScheduledAt(Instant.now().plus(14, ChronoUnit.DAYS));
        examReq.setInstructions("Comprehensive closed-book assessment on graph algorithms and asymptotics.");

        MvcResult examResult = mockMvc.perform(post("/api/v1/examinations")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(examReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Midterm Examination - Algorithms & Graph Theory"))
                .andReturn();

        Long examId = objectMapper.readTree(examResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Student views scheduled examination
        mockMvc.perform(get("/api/v1/examinations/subject/" + seededSubject.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(examId));

        // 3. Faculty records official grade including mandatory 'grade' letter conforming to RecordGradeRequest
        RecordGradeRequest gradeReq = new RecordGradeRequest();
        gradeReq.setExaminationId(examId);
        gradeReq.setStudentId(student.getId());
        gradeReq.setMarksObtained(BigDecimal.valueOf(94.5));
        gradeReq.setGrade("A+");
        gradeReq.setRemarks("Outstanding clarity on dynamic programming and graph traversals.");

        mockMvc.perform(post("/api/v1/examinations/grades")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marksObtained").value(94.5))
                .andExpect(jsonPath("$.data.grade").value("A+"));

        // 4. Student retrieves official grade report
        mockMvc.perform(get("/api/v1/examinations/my-results")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].marksObtained").value(94.5))
                .andExpect(jsonPath("$.data[0].grade").value("A+"));
    }

    @Test
    @Order(5)
    @DisplayName("Academic Lifecycle E2E: Live session scheduling, joining, and status transitions")
    void test05_CrossRoleAcademicWorkflow_LiveSessions() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");
        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");

        // 1. Faculty schedules live classroom session
        CreateLiveSessionRequest sessionReq = new CreateLiveSessionRequest();
        sessionReq.setSubjectId(seededSubject.getId());
        sessionReq.setTitle("Lecture 14: Network Flow & Max-Flow Min-Cut Theorem");
        sessionReq.setDescription("Live interactive walkthrough of Ford-Fulkerson and Edmonds-Karp algorithms.");
        sessionReq.setMeetingLink("https://meet.google.com/xyz-educ-vc");
        sessionReq.setScheduledStart(Instant.now().plus(2, ChronoUnit.HOURS));
        sessionReq.setScheduledEnd(Instant.now().plus(4, ChronoUnit.HOURS));

        MvcResult sessionRes = mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Lecture 14: Network Flow & Max-Flow Min-Cut Theorem"))
                .andReturn();

        Long sessionId = objectMapper.readTree(sessionRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Student lists scheduled sessions for subject
        mockMvc.perform(get("/api/v1/sessions/subject/" + seededSubject.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(sessionId))
                .andExpect(jsonPath("$.data.content[0].meetingLink").value("https://meet.google.com/xyz-educ-vc"));

        // 3. Faculty transitions session status to LIVE then COMPLETED
        mockMvc.perform(patch("/api/v1/sessions/" + sessionId + "/status")
                        .param("status", "LIVE")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LIVE"));

        mockMvc.perform(patch("/api/v1/sessions/" + sessionId + "/status")
                        .param("status", "COMPLETED")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    // =========================================================================
    // SECTION 4: FILE STORAGE, SIGNED URLS & OWNERSHIP SECURITY
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("File Storage E2E: Multipart upload, signed download URL, and deletion authorization")
    void test06_FileStorageLifecycle_EndToEnd() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");
        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");

        // 1. Upload valid PDF document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "algorithms_notes.pdf",
                "application/pdf",
                "%PDF-1.4 Mock PDF syllabus content for testing".getBytes()
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/storage/upload")
                        .file(file)
                        .param("folder", "materials")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFilename").value("algorithms_notes.pdf"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andReturn();

        String storagePath = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).path("data").path("storagePath").asText();
        assertNotNull(storagePath);

        // 2. Generate signed download URL
        mockMvc.perform(get("/api/v1/storage/signed-download-url")
                        .param("path", storagePath)
                        .param("expiresIn", "1800")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Student unauthorized deletion attempt must be rejected (only Faculty/Admin permitted)
        mockMvc.perform(delete("/api/v1/storage/files")
                        .param("path", storagePath)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 4. Authorized Faculty deletes file
        mockMvc.perform(delete("/api/v1/storage/files")
                        .param("path", storagePath)
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // SECTION 5: FORUM DISCUSSIONS & NOTIFICATIONS
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Forum & Notifications E2E: Question posting, answering, faculty endorsement, and read lifecycle")
    void test07_ForumAndNotifications_EndToEnd() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");
        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");

        // 1. Student posts question in discussion forum
        CreateQuestionRequest qReq = new CreateQuestionRequest();
        qReq.setSubjectId(seededSubject.getId());
        qReq.setTitle("Time Complexity of Fibonacci Heap Decrease-Key");
        qReq.setBody("Is the amortized time O(1) or worst-case O(log n)?");

        MvcResult qResult = mockMvc.perform(post("/api/v1/discussions/questions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Time Complexity of Fibonacci Heap Decrease-Key"))
                .andReturn();

        Long questionId = objectMapper.readTree(qResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Faculty posts answer
        CreateAnswerRequest ansReq = new CreateAnswerRequest();
        ansReq.setBody("Decrease-Key in a Fibonacci Heap runs in O(1) amortized time using cascading cuts.");

        MvcResult ansResult = mockMvc.perform(post("/api/v1/discussions/questions/" + questionId + "/answers")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ansReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value(org.hamcrest.Matchers.containsString("O(1) amortized")))
                .andReturn();

        Long answerId = objectMapper.readTree(ansResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Faculty endorses answer
        mockMvc.perform(patch("/api/v1/discussions/answers/" + answerId + "/endorse")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endorsed").value(true));

        // 4. Student marks question as resolved
        mockMvc.perform(patch("/api/v1/discussions/questions/" + questionId + "/resolve")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. In-App Notifications: Trigger and read
        Notification n = new Notification();
        n.setUser(student);
        n.setTitle("New Exam Scheduled");
        n.setMessage("Midterm examination scheduled for CS301.");
        n.setIsRead(false);
        Notification savedNotification = notificationRepository.save(n);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(patch("/api/v1/notifications/" + savedNotification.getId() + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // SECTION 6: SECURITY & NEGATIVE AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Security Negative E2E: Unauthorized role escalation, IDOR attempts, and unauthenticated access")
    void test08_NegativeSecurityAndRBAC_EndToEnd() throws Exception {
        User student = userRepository.findByEmail("student.e2e@virtualclassroom.edu").orElseThrow();
        User faculty = userRepository.findByEmail("faculty.e2e@virtualclassroom.edu").orElseThrow();

        String studentToken = obtainToken(student.getEmail(), "StudentPass@2025!");
        String facultyToken = obtainToken(faculty.getEmail(), "FacultyPass@2025!");

        // 1. Student attempting Admin metrics -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/metrics")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. Faculty attempting Admin approvals -> 403 Forbidden
        mockMvc.perform(post("/api/v1/admin/approvals")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[1]}"))
                .andExpect(status().isForbidden());

        // 3. Student attempting to grade an assignment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/assignments/submissions/1/grade")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradePoints\": 100}"))
                .andExpect(status().isForbidden());

        // 4. Unauthenticated call to protected endpoint -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        // 5. Expired or invalid Bearer token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid_tampered_jwt_token_payload"))
                .andExpect(status().isUnauthorized());
    }
}
