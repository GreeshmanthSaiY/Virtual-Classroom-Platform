package com.virtualclassroom.controller;

import com.virtualclassroom.dto.ApiResponse;
import com.virtualclassroom.dto.FileUploadResponse;
import com.virtualclassroom.dto.SignedUploadUrlRequest;
import com.virtualclassroom.dto.SignedUploadUrlResponse;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.security.UserPrincipal;
import com.virtualclassroom.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "File Storage", description = "File Uploads, Signed URLs & Document Retrieval")
public class StorageController {

    private final FileStorageService storageService;
    private final UserRepository userRepository;

    public StorageController(FileStorageService storageService, UserRepository userRepository) {
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a document, study material, or assignment attachment")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = (userRepository != null && principal != null) ? userRepository.getReferenceById(principal.getId()) : null;
        FileUploadResponse response = storageService.uploadFile(file, folder, user);
        return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully"));
    }

    @PostMapping("/signed-upload-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate signed upload URL for direct client storage upload")
    public ResponseEntity<ApiResponse<SignedUploadUrlResponse>> getSignedUploadUrl(
            @Valid @RequestBody SignedUploadUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = (userRepository != null && principal != null) ? userRepository.getReferenceById(principal.getId()) : null;
        SignedUploadUrlResponse response = storageService.generateSignedUploadUrl(request, user);
        return ResponseEntity.ok(ApiResponse.success(response, "Signed upload URL generated"));
    }

    @GetMapping("/signed-download-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate a time-limited signed URL for secure file download")
    public ResponseEntity<ApiResponse<String>> getSignedDownloadUrl(
            @RequestParam("path") String storagePath,
            @RequestParam(value = "expiresIn", defaultValue = "3600") int expiresIn
    ) {
        String signedUrl = storageService.generateSignedDownloadUrl(storagePath, expiresIn);
        return ResponseEntity.ok(ApiResponse.success(signedUrl, "Signed download URL generated"));
    }

    @GetMapping("/files/**")
    @Operation(summary = "Retrieve or stream locally stored file")
    public ResponseEntity<Resource> getLocalFile(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/v1/storage/files/";
        int index = fullPath.indexOf(prefix);
        if (index == -1) {
            return ResponseEntity.badRequest().build();
        }
        String storagePath = fullPath.substring(index + prefix.length());
        Resource resource = storageService.loadFileAsResource(storagePath);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ignored) {}

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/files")
    @PreAuthorize("hasAnyAuthority('ROLE_FACULTY', 'ROLE_ADMIN')")
    @Operation(summary = "Delete an uploaded file from storage")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("path") String storagePath,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = (userRepository != null && principal != null) ? userRepository.getReferenceById(principal.getId()) : null;
        storageService.deleteFile(storagePath, user);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
    }
}
