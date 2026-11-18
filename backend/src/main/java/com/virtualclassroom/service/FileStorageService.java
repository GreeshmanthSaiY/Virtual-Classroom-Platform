package com.virtualclassroom.service;

import com.virtualclassroom.dto.FileUploadResponse;
import com.virtualclassroom.dto.SignedUploadUrlRequest;
import com.virtualclassroom.dto.SignedUploadUrlResponse;
import com.virtualclassroom.exception.StorageOperationException;
import com.virtualclassroom.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public static final int DEFAULT_SIGNED_DOWNLOAD_EXPIRY_SECONDS = 900; // 15 minutes
    public static final int MIN_SIGNED_DOWNLOAD_EXPIRY_SECONDS = 60;
    public static final int MAX_SIGNED_DOWNLOAD_EXPIRY_SECONDS = 3600; // 1 hour max

    private static final Pattern SAFE_STORAGE_PATH_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]+(/[a-zA-Z0-9._-]+)+$");

    // Aligned 1:1 with the 14 allowed_mime_types configured on Supabase bucket 'academic-assets'
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
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
            "video/mp4",
            "video/webm"
    );

    public static final Set<String> ALLOWED_BUCKETS = Set.of(
            "academic-assets",
            "study-materials",
            "submissions",
            "profile-photos",
            "course-documents"
    );

    private final String provider;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String supabaseBucket;
    private final Path localBaseDir;
    private final long maxFileSizeBytes;
    private final RestTemplate restTemplate;
    private final Environment environment;

    public FileStorageService(
            String provider,
            String supabaseUrl,
            String supabaseKey,
            String supabaseBucket,
            String localBaseDir,
            long maxFileSizeBytes
    ) {
        this(provider, supabaseUrl, supabaseKey, supabaseBucket, localBaseDir, maxFileSizeBytes, null, new RestTemplate());
    }

    public FileStorageService(
            String provider,
            String supabaseUrl,
            String supabaseKey,
            String supabaseBucket,
            String localBaseDir,
            long maxFileSizeBytes,
            Environment environment
    ) {
        this(provider, supabaseUrl, supabaseKey, supabaseBucket, localBaseDir, maxFileSizeBytes, environment, new RestTemplate());
    }

    @Autowired
    public FileStorageService(
            @Value("${app.storage.provider:local}") String provider,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.key:}") String supabaseKey,
            @Value("${app.storage.supabase.bucket:academic-assets}") String supabaseBucket,
            @Value("${app.storage.local.base-dir:./uploads}") String localBaseDir,
            @Value("${app.storage.max-file-size:52428800}") long maxFileSizeBytes,
            @Autowired(required = false) Environment environment,
            @Autowired(required = false) RestTemplate restTemplate
    ) {
        this.provider = (provider != null && !provider.isBlank()) ? provider.trim() : "local";
        this.supabaseUrl = (supabaseUrl != null) ? stripTrailingSlash(supabaseUrl.trim()) : "";
        this.supabaseKey = (supabaseKey != null) ? supabaseKey.trim() : "";
        this.supabaseBucket = (supabaseBucket != null && !supabaseBucket.isBlank()) ? supabaseBucket.trim() : "academic-assets";
        this.localBaseDir = Paths.get(localBaseDir != null ? localBaseDir : "./uploads").toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes > 0 ? maxFileSizeBytes : 52428800L; // 50MB default
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.environment = environment;

        if (!ALLOWED_BUCKETS.contains(this.supabaseBucket)) {
            throw new IllegalArgumentException(
                    "Configured storage bucket '" + this.supabaseBucket + "' is not in the approved bucket allowlist: " + ALLOWED_BUCKETS
            );
        }

        if (isProductionProfile()) {
            if (!isSupabaseConfigured()) {
                throw new IllegalStateException(
                        "Production configuration error: Supabase cloud storage (SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY) is required in production profile. Local filesystem fallback is strictly disabled in production."
                );
            }
        } else if (isStagingProfile()) {
            if (!isSupabaseConfigured()) {
                throw new IllegalStateException(
                        "Staging configuration error: Supabase cloud storage (SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY) is required in staging profile. Local filesystem fallback is strictly disabled in staging."
                );
            }
        }

        if (!isCloudStorageRequiredProfile()) {
            try {
                Files.createDirectories(this.localBaseDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize local storage directory: " + this.localBaseDir, e);
            }
        }
    }

    public FileUploadResponse uploadFile(MultipartFile file, String folder, User uploader) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty or non-existent file");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "File size (" + file.getSize() + " bytes) exceeds maximum allowable limit (" + maxFileSizeBytes + " bytes)"
            );
        }

        String rawContentType = file.getContentType();
        String contentType = (rawContentType != null) ? rawContentType.toLowerCase(Locale.ROOT).trim() : "application/octet-stream";
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported media type: " + contentType + ". Allowed file types include PDF, Office documents, text, images, and video."
            );
        }

        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String folderPath = (folder != null && !folder.isBlank()) ? sanitizePathSegment(folder) : "general";
        String userPrefix = (uploader != null && uploader.getId() != null) ? uploader.getId() + "/" : "";
        String storagePath = folderPath + "/" + userPrefix + UUID.randomUUID() + "-" + sanitizedFilename;
        validateStoragePath(storagePath);

        if (isSupabaseConfigured()) {
            return uploadToSupabase(file, storagePath, sanitizedFilename, contentType);
        }

        if (isCloudStorageRequiredProfile()) {
            throw new StorageOperationException("Cloud storage is required in staging/production environments; local fallback is disabled.");
        }

        return uploadToLocalStorage(file, storagePath, sanitizedFilename, contentType);
    }

    public SignedUploadUrlResponse generateSignedUploadUrl(SignedUploadUrlRequest request, User uploader) {
        if (request == null || request.getFilename() == null || request.getFilename().isBlank()) {
            throw new IllegalArgumentException("Filename is required to generate a signed upload URL");
        }

        String sanitizedFilename = sanitizeFilename(request.getFilename());
        String folderPath = sanitizePathSegment(request.getFolder());
        String userPrefix = (uploader != null && uploader.getId() != null) ? uploader.getId() + "/" : "";
        String storagePath = folderPath + "/" + userPrefix + UUID.randomUUID() + "-" + sanitizedFilename;
        validateStoragePath(storagePath);

        int expiry = clampSignedUrlExpiry(
                request.getExpiresInSeconds() > 0 ? request.getExpiresInSeconds() : DEFAULT_SIGNED_DOWNLOAD_EXPIRY_SECONDS
        );

        if (isSupabaseConfigured()) {
            String apiUrl = supabaseUrl + "/storage/v1/object/upload/sign/" + supabaseBucket + "/" + storagePath;
            try {
                HttpHeaders headers = createSupabaseHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-upsert", "false");

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Collections.emptyMap(), headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                if (!response.getStatusCode().is2xxSuccessful()
                        || response.getBody() == null
                        || response.getBody().get("url") == null) {
                    throw new StorageOperationException("Supabase Storage signed upload URL response was invalid.");
                }

                String relativeSignedUrl = String.valueOf(response.getBody().get("url"));
                String signedUploadUrl = supabaseUrl + "/storage/v1" + relativeSignedUrl;

                // Never emit /storage/v1/object/public/... for private buckets.
                // Clients must request short-lived signed download URLs via the authenticated backend endpoint.
                String authenticatedDownloadRef = "/api/v1/storage/signed-download-url?path=" + storagePath;

                return SignedUploadUrlResponse.builder()
                        .uploadUrl(signedUploadUrl)
                        .storagePath(storagePath)
                        .publicOrDownloadUrl(authenticatedDownloadRef)
                        .expiresInSeconds(expiry)
                        .build();
            } catch (StorageOperationException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to generate Supabase signed upload URL for path '{}'", storagePath);
                throw new StorageOperationException("Failed to generate signed upload URL from cloud storage.", e);
            }
        }

        if (isCloudStorageRequiredProfile()) {
            throw new StorageOperationException("Cloud storage is required in staging/production environments; local fallback is disabled.");
        }

        // Local development simulation
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
        validateStoragePath(storagePath);
        int expiry = clampSignedUrlExpiry(expirySeconds > 0 ? expirySeconds : DEFAULT_SIGNED_DOWNLOAD_EXPIRY_SECONDS);

        if (isSupabaseConfigured()) {
            String apiUrl = supabaseUrl + "/storage/v1/object/sign/" + supabaseBucket + "/" + storagePath;
            try {
                HttpHeaders headers = createSupabaseHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of("expiresIn", expiry);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful()
                        && response.getBody() != null
                        && response.getBody().get("signedURL") != null) {
                    String signedPath = String.valueOf(response.getBody().get("signedURL"));
                    return supabaseUrl + "/storage/v1" + signedPath;
                }

                throw new StorageOperationException("Supabase Storage did not return a valid signedURL.");
            } catch (StorageOperationException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to generate Supabase signed download URL for path '{}'", storagePath);
                throw new StorageOperationException("Failed to generate signed download URL from cloud storage.", e);
            }
        }

        if (isCloudStorageRequiredProfile()) {
            throw new StorageOperationException("Cloud storage is required in staging/production environments; local fallback is disabled.");
        }

        return "/api/v1/storage/files/" + storagePath;
    }

    public void deleteFile(String storagePath) {
        deleteFile(storagePath, null);
    }

    public void deleteFile(String storagePath, User actor) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        validateStoragePath(storagePath);

        if (actor != null && actor.getRole() != com.virtualclassroom.model.Role.ROLE_ADMIN) {
            String userNamespace = "/" + actor.getId() + "/";
            String userPrefix = actor.getId() + "/";
            if (!storagePath.contains(userNamespace) && !storagePath.startsWith(userPrefix)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access denied: You are not authorized to delete files belonging to other users"
                );
            }
        }

        if (isSupabaseConfigured()) {
            try {
                String apiUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + storagePath;
                HttpHeaders headers = createSupabaseHeaders();
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                restTemplate.exchange(apiUrl, HttpMethod.DELETE, entity, Void.class);
            } catch (Exception e) {
                log.error("Failed to delete object '{}' from Supabase Storage", storagePath);
                if (isCloudStorageRequiredProfile()) {
                    throw new StorageOperationException("Failed to delete object from cloud storage.", e);
                }
            }
            return;
        }

        if (isCloudStorageRequiredProfile()) {
            throw new StorageOperationException("Cloud storage is required in staging/production environments.");
        }

        // Delete from local directory in local dev mode
        try {
            Path targetFile = localBaseDir.resolve(storagePath).normalize();
            if (targetFile.startsWith(localBaseDir) && Files.exists(targetFile)) {
                Files.delete(targetFile);
            }
        } catch (IOException ignored) {
        }
    }

    public Resource loadFileAsResource(String storagePath) {
        if (isSupabaseConfigured() || isCloudStorageRequiredProfile()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Direct local file streaming is disabled when cloud storage is active. Use signed download URLs."
            );
        }
        validateStoragePath(storagePath);

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

    /**
     * Strictly validates canonical storage object paths to prevent path traversal,
     * URL manipulation, and injection into Supabase Storage REST endpoints.
     */
    public void validateStoragePath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required");
        }
        String lower = storagePath.toLowerCase(Locale.ROOT);
        if (storagePath.contains("..")
                || storagePath.contains("\\")
                || storagePath.startsWith("/")
                || storagePath.endsWith("/")
                || storagePath.contains("//")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c")
                || storagePath.contains("\0")) {
            throw new SecurityException("Unauthorized file path traversal attempt detected");
        }
        if (!SAFE_STORAGE_PATH_PATTERN.matcher(storagePath).matches()) {
            throw new IllegalArgumentException("Invalid storage path format");
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
            HttpHeaders headers = createSupabaseHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.set("x-upsert", "false");

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase Storage upload returned non-2xx status: {}", response.getStatusCode());
                throw new StorageOperationException("Cloud storage upload rejected with status: " + response.getStatusCode());
            }

            // Private bucket: never construct /storage/v1/object/public/... URLs.
            // Return a short-lived signed download URL for immediate authenticated display,
            // alongside the canonical storagePath for database persistence.
            String signedDownloadUrl = generateSignedDownloadUrl(storagePath, DEFAULT_SIGNED_DOWNLOAD_EXPIRY_SECONDS);

            return FileUploadResponse.builder()
                    .fileUrl(signedDownloadUrl)
                    .storagePath(storagePath)
                    .originalFilename(filename)
                    .mimeType(mimeType)
                    .fileSizeBytes(file.getSize())
                    .build();
        } catch (StorageOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file '{}' to Supabase Storage", storagePath);
            throw new StorageOperationException("Failed to upload file to cloud storage.", e);
        }
    }

    private HttpHeaders createSupabaseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseKey);
        headers.set("apikey", supabaseKey);
        return headers;
    }

    private int clampSignedUrlExpiry(int requestedSeconds) {
        return Math.min(Math.max(requestedSeconds, MIN_SIGNED_DOWNLOAD_EXPIRY_SECONDS), MAX_SIGNED_DOWNLOAD_EXPIRY_SECONDS);
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public boolean isProductionProfile() {
        return environment != null && (environment.matchesProfiles("prod") || environment.matchesProfiles("production"));
    }

    public boolean isStagingProfile() {
        return environment != null && environment.matchesProfiles("staging");
    }

    public boolean isCloudStorageRequiredProfile() {
        return isProductionProfile() || isStagingProfile();
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
        String clean = segment.replaceAll("[^a-zA-Z0-9_-]", "");
        return clean.isBlank() ? "general" : clean;
    }
}
