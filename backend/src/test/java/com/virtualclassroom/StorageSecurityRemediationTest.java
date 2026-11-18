package com.virtualclassroom;

import com.virtualclassroom.controller.StorageController;
import com.virtualclassroom.dto.FileUploadResponse;
import com.virtualclassroom.dto.SignedUploadUrlRequest;
import com.virtualclassroom.dto.SignedUploadUrlResponse;
import com.virtualclassroom.exception.StorageOperationException;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.FileStorageService;
import com.virtualclassroom.service.StorageAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Milestone 18.3 - Storage Security Remediation Verification Suite
 * Proves official Supabase Storage REST API usage, fail-closed error handling,
 * strict path validation, private download URL enforcement, and domain-table
 * ownership authorization across Student, Faculty, and Admin roles.
 */
class StorageSecurityRemediationTest {

    private static final String SUPABASE_URL = "https://zrbpjcfssbtxxgrjtsca.supabase.co";
    private static final String FAKE_SERVICE_KEY = "test-service-role-key-not-a-real-secret";
    private static final String BUCKET = "academic-assets";

    @TempDir
    Path tempDir;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private MockEnvironment stagingEnv;
    private FileStorageService cloudStorageService;

    private AssignmentSubmissionRepository submissionRepository;
    private StudyMaterialRepository studyMaterialRepository;
    private AssignmentRepository assignmentRepository;
    private StudentSubjectEnrollmentRepository enrollmentRepository;
    private FacultySubjectAssignmentRepository facultyAssignmentRepository;
    private StorageAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        stagingEnv = new MockEnvironment();
        stagingEnv.setActiveProfiles("staging");

        cloudStorageService = new FileStorageService(
                "supabase", SUPABASE_URL, FAKE_SERVICE_KEY, BUCKET,
                tempDir.toString(), 52428800L, stagingEnv, restTemplate
        );

        submissionRepository = mock(AssignmentSubmissionRepository.class);
        studyMaterialRepository = mock(StudyMaterialRepository.class);
        assignmentRepository = mock(AssignmentRepository.class);
        enrollmentRepository = mock(StudentSubjectEnrollmentRepository.class);
        facultyAssignmentRepository = mock(FacultySubjectAssignmentRepository.class);

        when(submissionRepository.findBySubmissionStoragePath(anyString())).thenReturn(Collections.emptyList());
        when(studyMaterialRepository.findByStoragePath(anyString())).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByAttachmentStoragePath(anyString())).thenReturn(Collections.emptyList());

        authorizationService = new StorageAuthorizationService(
                submissionRepository,
                studyMaterialRepository,
                assignmentRepository,
                enrollmentRepository,
                facultyAssignmentRepository
        );
    }

    // =========================================================================
    // 1. Official Supabase Storage REST API & Private URL Enforcement
    // =========================================================================

    @Test
    @DisplayName("Signed download URL calls official POST /storage/v1/object/sign with Bearer + apikey and clamped expiry")
    void generateSignedDownloadUrl_UsesOfficialSupabaseApiWithRequiredHeaders() {
        String storagePath = "study-materials/10/lecture-notes.pdf";
        String expectedEndpoint = SUPABASE_URL + "/storage/v1/object/sign/" + BUCKET + "/" + storagePath;

        mockServer.expect(requestTo(expectedEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_SERVICE_KEY))
                .andExpect(header("apikey", FAKE_SERVICE_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.expiresIn").value(3600)) // Clamped down from 999999 to max 3600s
                .andRespond(withSuccess(
                        "{\"signedURL\":\"/object/sign/academic-assets/study-materials/10/lecture-notes.pdf?token=jwt-tok\"}",
                        MediaType.APPLICATION_JSON
                ));

        String result = cloudStorageService.generateSignedDownloadUrl(storagePath, 999999);

        mockServer.verify();
        assertEquals(
                SUPABASE_URL + "/storage/v1/object/sign/academic-assets/study-materials/10/lecture-notes.pdf?token=jwt-tok",
                result
        );
        assertFalse(result.contains("/object/public/"), "Must never return public-style URLs for private bucket");
    }

    @Test
    @DisplayName("Upload to Supabase sends x-upsert=false + apikey and returns signed download URL (never /object/public/)")
    void uploadFile_ToSupabase_ReturnsSignedUrlNotPublicUrl() {
        User faculty = User.builder().id(42L).role(Role.ROLE_FACULTY).status(UserStatus.APPROVED).build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "PDF bytes".getBytes()
        );

        mockServer.expect(requestTo(startsWith(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/study-materials/42/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_SERVICE_KEY))
                .andExpect(header("apikey", FAKE_SERVICE_KEY))
                .andExpect(header("x-upsert", "false"))
                .andRespond(withSuccess("{\"Key\":\"academic-assets/ok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(startsWith(SUPABASE_URL + "/storage/v1/object/sign/" + BUCKET + "/study-materials/42/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_SERVICE_KEY))
                .andExpect(header("apikey", FAKE_SERVICE_KEY))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andRespond(withSuccess(
                        "{\"signedURL\":\"/object/sign/academic-assets/study-materials/42/file.pdf?token=signed\"}",
                        MediaType.APPLICATION_JSON
                ));

        FileUploadResponse response = cloudStorageService.uploadFile(file, "study-materials", faculty);

        mockServer.verify();
        assertNotNull(response);
        assertTrue(response.getStoragePath().startsWith("study-materials/42/"));
        assertTrue(response.getFileUrl().contains("/object/sign/"));
        assertFalse(response.getFileUrl().contains("/object/public/"));
    }

    @Test
    @DisplayName("Signed upload URL uses official /object/upload/sign endpoint and omits /object/public/ download URL")
    void generateSignedUploadUrl_NeverEmitsPublicUrl() {
        User student = User.builder().id(7L).role(Role.ROLE_STUDENT).status(UserStatus.APPROVED).build();
        SignedUploadUrlRequest request = new SignedUploadUrlRequest("hw1.pdf", "submissions", 900);

        mockServer.expect(requestTo(startsWith(SUPABASE_URL + "/storage/v1/object/upload/sign/" + BUCKET + "/submissions/7/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_SERVICE_KEY))
                .andExpect(header("apikey", FAKE_SERVICE_KEY))
                .andExpect(header("x-upsert", "false"))
                .andRespond(withSuccess(
                        "{\"url\":\"/object/upload/sign/academic-assets/submissions/7/hw1.pdf?token=up-tok\",\"token\":\"up-tok\"}",
                        MediaType.APPLICATION_JSON
                ));

        SignedUploadUrlResponse response = cloudStorageService.generateSignedUploadUrl(request, student);

        mockServer.verify();
        assertTrue(response.getUploadUrl().contains("/storage/v1/object/upload/sign/"));
        assertTrue(response.getPublicOrDownloadUrl().startsWith("/api/v1/storage/signed-download-url?path="));
        assertFalse(response.getPublicOrDownloadUrl().contains("/object/public/"));
    }

    // =========================================================================
    // 2. Fail-Closed Error Handling & Staging Credential Guard
    // =========================================================================

    @Test
    @DisplayName("Staging profile fails fast on missing Supabase credentials")
    void constructor_StagingMissingCredentials_ThrowsIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new FileStorageService("supabase", "", "", BUCKET, tempDir.toString(), 52428800L, stagingEnv)
        );
        assertTrue(ex.getMessage().contains("Staging configuration error"));
    }

    @Test
    @DisplayName("Upstream Supabase error fails closed with StorageOperationException without leaking service key")
    void generateSignedDownloadUrl_SupabaseFailure_FailsClosedWithoutSecretLeak() {
        String storagePath = "submissions/7/hw1.pdf";
        mockServer.expect(requestTo(SUPABASE_URL + "/storage/v1/object/sign/" + BUCKET + "/" + storagePath))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("upstream error"));

        StorageOperationException ex = assertThrows(StorageOperationException.class, () ->
                cloudStorageService.generateSignedDownloadUrl(storagePath, 900)
        );
        assertFalse(ex.getMessage().contains(FAKE_SERVICE_KEY), "Service key must never appear in exception message");
    }

    // =========================================================================
    // 3. Strict Path Validation
    // =========================================================================

    @Test
    @DisplayName("Path validation rejects traversal, encoded traversal, leading slashes, and query strings")
    void validateStoragePath_RejectsMaliciousPaths() {
        assertThrows(SecurityException.class, () -> cloudStorageService.validateStoragePath("../secret.pdf"));
        assertThrows(SecurityException.class, () -> cloudStorageService.validateStoragePath("submissions/../../etc/passwd"));
        assertThrows(SecurityException.class, () -> cloudStorageService.validateStoragePath("submissions/%2e%2e/secret.pdf"));
        assertThrows(SecurityException.class, () -> cloudStorageService.validateStoragePath("/submissions/1/a.pdf"));
        assertThrows(SecurityException.class, () -> cloudStorageService.validateStoragePath("submissions\\1\\a.pdf"));
        assertThrows(IllegalArgumentException.class, () -> cloudStorageService.validateStoragePath("submissions/1/a.pdf?download=1"));
        assertThrows(IllegalArgumentException.class, () -> cloudStorageService.validateStoragePath(""));
        assertDoesNotThrow(() -> cloudStorageService.validateStoragePath("submissions/10/550e8400-hw1.pdf"));
    }

    // =========================================================================
    // 4. Resource Ownership & Role Authorization Checks
    // =========================================================================

    @Test
    @DisplayName("Student A cannot access Student B's assignment submission")
    void authorizeDownload_StudentACannotAccessStudentBSubmission() {
        String path = "submissions/200/uuid-answer.pdf";
        User studentB = User.builder().id(200L).role(Role.ROLE_STUDENT).build();
        Subject subject = Subject.builder().id(1).build();
        Assignment assignment = Assignment.builder().id(10L).subject(subject).build();
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(55L).student(studentB).assignment(assignment).fileUrl(path).build();

        when(submissionRepository.findBySubmissionStoragePath(path)).thenReturn(List.of(submission));

        UserPrincipal studentAPrincipal = principal(100L, Role.ROLE_STUDENT);
        UserPrincipal studentBPrincipal = principal(200L, Role.ROLE_STUDENT);

        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeDownload(path, studentAPrincipal));
        assertDoesNotThrow(() -> authorizationService.authorizeDownload(path, studentBPrincipal));
    }

    @Test
    @DisplayName("Assigned faculty can access student submission; unassigned faculty is denied")
    void authorizeDownload_FacultySubjectScopedAccessToSubmission() {
        String path = "submissions/200/uuid-answer.pdf";
        Subject subject = Subject.builder().id(3).build();
        User assignedFaculty = User.builder().id(50L).role(Role.ROLE_FACULTY).build();
        Assignment assignment = Assignment.builder().id(10L).subject(subject).faculty(assignedFaculty).build();
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .id(55L).student(User.builder().id(200L).build()).assignment(assignment).fileUrl(path).build();

        when(submissionRepository.findBySubmissionStoragePath(path)).thenReturn(List.of(submission));
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(50L, 3)).thenReturn(true);
        when(facultyAssignmentRepository.existsByFacultyIdAndSubjectId(99L, 3)).thenReturn(false);

        assertDoesNotThrow(() -> authorizationService.authorizeDownload(path, principal(50L, Role.ROLE_FACULTY)));
        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeDownload(path, principal(99L, Role.ROLE_FACULTY)));
    }

    @Test
    @DisplayName("Study material download requires subject enrollment for students; admin always permitted")
    void authorizeDownload_StudyMaterialRequiresEnrollment() {
        String path = "study-materials/50/uuid-slides.pdf";
        Subject subject = Subject.builder().id(2).build();
        StudyMaterial material = StudyMaterial.builder()
                .id(12L).subject(subject).uploader(User.builder().id(50L).build()).fileUrl(path).build();

        when(studyMaterialRepository.findByStoragePath(path)).thenReturn(List.of(material));
        when(enrollmentRepository.existsByStudentIdAndSubjectId(100L, 2)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndSubjectId(101L, 2)).thenReturn(false);

        assertDoesNotThrow(() -> authorizationService.authorizeDownload(path, principal(100L, Role.ROLE_STUDENT)));
        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeDownload(path, principal(101L, Role.ROLE_STUDENT)));
        assertDoesNotThrow(() -> authorizationService.authorizeDownload(path, principal(1L, Role.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("Unlinked file fails closed for non-owner and succeeds for uploader or profile photo")
    void authorizeDownload_UnlinkedFilesFailClosed() {
        assertDoesNotThrow(() -> authorizationService.authorizeDownload("submissions/100/new.pdf", principal(100L, Role.ROLE_STUDENT)));
        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeDownload("submissions/200/new.pdf", principal(100L, Role.ROLE_STUDENT)));
        assertThrows(AccessDeniedException.class, () -> authorizationService.authorizeDownload("general/orphan.pdf", principal(100L, Role.ROLE_STUDENT)));
        assertDoesNotThrow(() -> authorizationService.authorizeDownload("profile-photos/200/avatar.png", principal(100L, Role.ROLE_STUDENT)));
    }

    @Test
    @DisplayName("StorageController enforces authorization before signing and blocks local file streaming in cloud mode")
    void storageController_EnforcesAuthAndBlocksLocalStreamingInCloudMode() {
        StorageController controller = new StorageController(
                cloudStorageService, mock(UserRepository.class), authorizationService
        );

        // Unauthorized student cannot obtain signed download URL
        assertThrows(AccessDeniedException.class, () ->
                controller.getSignedDownloadUrl("submissions/200/other.pdf", 900, principal(100L, Role.ROLE_STUDENT))
        );

        // Local file streaming endpoint returns 403 when cloud storage is active
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/storage/files/general/test.pdf");
        var resp = controller.getLocalFile(req);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    private UserPrincipal principal(Long id, Role role) {
        return new UserPrincipal(id, "user" + id + "@university.edu", "hash", "User " + id,
                role.name(), UserStatus.APPROVED, Collections.emptyList());
    }
}
