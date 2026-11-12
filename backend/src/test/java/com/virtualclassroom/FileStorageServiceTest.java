package com.virtualclassroom;

import com.virtualclassroom.dto.FileUploadResponse;
import com.virtualclassroom.dto.SignedUploadUrlRequest;
import com.virtualclassroom.dto.SignedUploadUrlResponse;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempStorageDir;

    private FileStorageService storageService;
    private User testUser;

    @BeforeEach
    void setUp() {
        storageService = new FileStorageService(
                "local", "", "", "academic-assets",
                tempStorageDir.toString(), 10 * 1024 * 1024 // 10MB limit
        );

        testUser = User.builder()
                .id(1L)
                .email("uploader@university.edu")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Uploads supported PDF file to local storage directory")
    void uploadFile_ValidPdf_SavesSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture1.pdf", "application/pdf", "Mock PDF file content".getBytes()
        );

        FileUploadResponse response = storageService.uploadFile(file, "materials", testUser);

        assertNotNull(response);
        assertNotNull(response.getFileUrl());
        assertTrue(response.getFileUrl().startsWith("/api/v1/storage/files/materials/"));
        assertEquals("lecture1.pdf", response.getOriginalFilename());
        assertEquals("application/pdf", response.getMimeType());
        assertTrue(response.getFileSizeBytes() > 0);
    }

    @Test
    @DisplayName("Rejects unsupported MIME types")
    void uploadFile_UnsupportedMimeType_ThrowsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious.exe", "application/x-msdownload", "binary".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.uploadFile(file, "materials", testUser)
        );
        assertTrue(ex.getMessage().contains("Unsupported media type"));
    }

    @Test
    @DisplayName("Enforces file size limit")
    void uploadFile_ExceedsSizeLimit_ThrowsIllegalArgument() {
        byte[] largePayload = new byte[11 * 1024 * 1024]; // 11MB (exceeds 10MB limit)
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largePayload
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.uploadFile(file, "materials", testUser)
        );
        assertTrue(ex.getMessage().contains("exceeds maximum allowable limit"));
    }

    @Test
    @DisplayName("Sanitizes filenames to prevent path traversal")
    void sanitizeFilename_PreventsPathTraversal() {
        String unsafe = "../../etc/passwd.pdf";
        String sanitized = storageService.sanitizeFilename(unsafe);

        assertFalse(sanitized.contains(".."));
        assertFalse(sanitized.contains("/"));
        assertFalse(sanitized.contains("\\"));
        assertTrue(sanitized.endsWith(".pdf"));
    }

    @Test
    @DisplayName("Generates signed upload URL response")
    void generateSignedUploadUrl_GeneratesValidResponse() {
        SignedUploadUrlRequest request = new SignedUploadUrlRequest("notes.docx", "coursework", 1800);

        SignedUploadUrlResponse response = storageService.generateSignedUploadUrl(request, testUser);

        assertNotNull(response);
        assertNotNull(response.getUploadUrl());
        assertNotNull(response.getStoragePath());
        assertTrue(response.getStoragePath().startsWith("coursework/"));
        assertEquals(1800, response.getExpiresInSeconds());
    }

    @Test
    @DisplayName("Loads uploaded file as Resource and prevents unauthorized path escape")
    void loadFileAsResource_ValidPath_ReturnsResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "syllabus.pdf", "application/pdf", "Syllabus Content".getBytes()
        );
        FileUploadResponse uploadResponse = storageService.uploadFile(file, "syllabi", testUser);

        Resource resource = storageService.loadFileAsResource(uploadResponse.getStoragePath());
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());

        // Path traversal attempt
        assertThrows(SecurityException.class, () ->
                storageService.loadFileAsResource("../../secret.txt")
        );
    }
}
