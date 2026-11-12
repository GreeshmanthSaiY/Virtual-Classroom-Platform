package com.virtualclassroom.service;

import com.virtualclassroom.dto.FileUploadResponse;
import com.virtualclassroom.dto.SignedUploadUrlRequest;
import com.virtualclassroom.dto.SignedUploadUrlResponse;
import com.virtualclassroom.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "video/mp4",
            "video/webm",
            "application/zip",
            "application/x-zip-compressed"
    );

    private final String provider;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String supabaseBucket;
    private final Path localBaseDir;
    private final long maxFileSizeBytes;
    private final RestTemplate restTemplate;

    public FileStorageService(
            @Value("${app.storage.provider:local}") String provider,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.key:}") String supabaseKey,
            @Value("${app.storage.supabase.bucket:academic-assets}") String supabaseBucket,
            @Value("${app.storage.local.base-dir:./uploads}") String localBaseDir,
            @Value("${app.storage.max-file-size:52428800}") long maxFileSizeBytes
    ) {
        this.provider = (provider != null && !provider.isBlank()) ? provider : "local";
        this.supabaseUrl = (supabaseUrl != null) ? supabaseUrl.trim() : "";
        this.supabaseKey = (supabaseKey != null) ? supabaseKey.trim() : "";
        this.supabaseBucket = (supabaseBucket != null && !supabaseBucket.isBlank()) ? supabaseBucket : "academic-assets";
        this.localBaseDir = Paths.get(localBaseDir != null ? localBaseDir : "./uploads").toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes > 0 ? maxFileSizeBytes : 52428800; // 50MB default
        this.restTemplate = new RestTemplate();

        try {
            Files.createDirectories(this.localBaseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize local storage directory: " + this.localBaseDir, e);
        }
    }

    public FileUploadResponse uploadFile(MultipartFile file, String folder, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty or non-existent file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size (" + file.getSize() + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)");
        }

        String rawContentType = file.getContentType();
        String contentType = (rawContentType != null) ? rawContentType.toLowerCase() : "application/octet-stream";
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported media type: " + contentType + ". Allowed file types include PDF, Office documents, text, images, and video.");
        }

        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String folderPath = (folder != null && !folder.isBlank()) ? sanitizePathSegment(folder) : "general";
        String storagePath = folderPath + "/" + UUID.randomUUID() + "-" + sanitizedFilename;

        if (isSupabaseConfigured()) {
            return uploadToSupabase(file, storagePath, sanitizedFilename, contentType);
        } else {
            return uploadToLocalStorage(file, storagePath, sanitizedFilename, contentType);
        }
    }

    public SignedUploadUrlResponse generateSignedUploadUrl(SignedUploadUrlRequest request, User uploader) {
        String sanitizedFilename = sanitizeFilename(request.getFilename());
        String folderPath = sanitizePathSegment(request.getFolder());
        String storagePath = folderPath + "/" + UUID.randomUUID() + "-" + sanitizedFilename;
        int expiry = request.getExpiresInSeconds() > 0 ? request.getExpiresInSeconds() : 3600;

        if (isSupabaseConfigured()) {
            String apiUrl = supabaseUrl + "/storage/v1/object/upload/sign/" + supabaseBucket + "/" + storagePath;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(supabaseKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of("expiresIn", expiry);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                String signedUrl = (response.getBody() != null && response.getBody().containsKey("url"))
                        ? supabaseUrl + "/storage/v1" + response.getBody().get("url")
                        : apiUrl;

                return SignedUploadUrlResponse.builder()
                        .uploadUrl(signedUrl)
                        .storagePath(storagePath)
                        .publicOrDownloadUrl(supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + storagePath)
                        .expiresInSeconds(expiry)
                        .build();
            } catch (Exception e) {
                // Graceful fallback to local upload endpoint if remote Supabase fails
            }
        }

        // Local simulation / fallback
        String localUploadUrl = "/api/v1/storage/upload?folder=" + folderPath;
        String localDownloadUrl = "/api/v1/storage/files/" + storagePath;
        return SignedUploadUrlResponse.builder()
                .uploadUrl(localUploadUrl)
                .storagePath(storagePath)
                .publicOrDownloadUrl(localDownloadUrl)
                .expiresInSeconds(expiry)
                .build();
    }

    public String generateSignedDownloadUrl(String storagePath, int expirySeconds) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required");
        }
        int expiry = expirySeconds > 0 ? expirySeconds : 3600;

        if (isSupabaseConfigured()) {
            String apiUrl = supabaseUrl + "/storage/v1/object/sign/" + supabaseBucket + "/" + storagePath;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(supabaseKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of("expiresIn", expiry);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                if (response.getBody() != null && response.getBody().containsKey("signedURL")) {
                    return supabaseUrl + "/storage/v1" + response.getBody().get("signedURL");
                }
            } catch (Exception e) {
                // Fall back to direct local reference
            }
        }

        return "/api/v1/storage/files/" + storagePath;
    }

    public void deleteFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        if (isSupabaseConfigured()) {
            try {
                String apiUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + storagePath;
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(supabaseKey);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                restTemplate.exchange(apiUrl, HttpMethod.DELETE, entity, Void.class);
            } catch (Exception ignored) {
            }
        }

        // Delete from local directory if present
        try {
            Path targetFile = localBaseDir.resolve(storagePath).normalize();
            if (targetFile.startsWith(localBaseDir) && Files.exists(targetFile)) {
                Files.delete(targetFile);
            }
        } catch (IOException ignored) {
        }
    }

    public Resource loadFileAsResource(String storagePath) {
        try {
            Path targetFile = localBaseDir.resolve(storagePath).normalize();
            if (!targetFile.startsWith(localBaseDir)) {
                throw new SecurityException("Unauthorized file path access attempt");
            }
            Resource resource = new UrlResource(targetFile.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found or unreadable: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path: " + storagePath, e);
        }
    }

    private FileUploadResponse uploadToLocalStorage(MultipartFile file, String storagePath, String filename, String mimeType) {
        try {
            Path targetPath = localBaseDir.resolve(storagePath).normalize();
            if (!targetPath.startsWith(localBaseDir)) {
                throw new SecurityException("File path traversal attempt detected");
            }

            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/storage/files/" + storagePath;

            return FileUploadResponse.builder()
                    .fileUrl(fileUrl)
                    .storagePath(storagePath)
                    .originalFilename(filename)
                    .mimeType(mimeType)
                    .fileSizeBytes(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist uploaded file locally: " + filename, e);
        }
    }

    private FileUploadResponse uploadToSupabase(MultipartFile file, String storagePath, String filename, String mimeType) {
        try {
            String apiUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + storagePath;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(supabaseKey);
            headers.setContentType(MediaType.parseMediaType(mimeType));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // If Supabase upload fails, fall back locally to prevent user breakage
                return uploadToLocalStorage(file, storagePath, filename, mimeType);
            }

            String fileUrl = supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + storagePath;

            return FileUploadResponse.builder()
                    .fileUrl(fileUrl)
                    .storagePath(storagePath)
                    .originalFilename(filename)
                    .mimeType(mimeType)
                    .fileSizeBytes(file.getSize())
                    .build();
        } catch (Exception e) {
            // Fall back locally
            return uploadToLocalStorage(file, storagePath, filename, mimeType);
        }
    }

    public boolean isSupabaseConfigured() {
        return "supabase".equalsIgnoreCase(provider)
                && !supabaseUrl.isBlank()
                && !supabaseKey.isBlank();
    }

    public String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed-file.bin";
        }
        String clean = Paths.get(originalFilename).getFileName().toString();
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.startsWith(".")) {
            clean = "file" + clean;
        }
        return clean;
    }

    public String sanitizePathSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "general";
        }
        return segment.replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
