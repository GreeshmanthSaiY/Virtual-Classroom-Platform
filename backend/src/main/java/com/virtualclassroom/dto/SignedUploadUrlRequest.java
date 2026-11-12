package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to generate signed URL for client upload")
public class SignedUploadUrlRequest {

    @NotBlank(message = "Original filename is required")
    @Schema(description = "Original filename including extension", example = "homework1.pdf")
    private String filename;

    @Schema(description = "Target storage folder or category", example = "assignments")
    private String folder = "uploads";

    @Schema(description = "URL expiration time in seconds (default 3600)", example = "3600")
    private int expiresInSeconds = 3600;

    public SignedUploadUrlRequest() {}

    public SignedUploadUrlRequest(String filename, String folder, int expiresInSeconds) {
        this.filename = filename;
        this.folder = folder != null ? folder : "uploads";
        this.expiresInSeconds = expiresInSeconds > 0 ? expiresInSeconds : 3600;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
