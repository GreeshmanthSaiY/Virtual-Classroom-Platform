package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signed URL upload metadata response")
public class SignedUploadUrlResponse {

    private String uploadUrl;
    private String storagePath;
    private String publicOrDownloadUrl;
    private int expiresInSeconds;

    public SignedUploadUrlResponse() {}

    public SignedUploadUrlResponse(String uploadUrl, String storagePath, String publicOrDownloadUrl, int expiresInSeconds) {
        this.uploadUrl = uploadUrl;
        this.storagePath = storagePath;
        this.publicOrDownloadUrl = publicOrDownloadUrl;
        this.expiresInSeconds = expiresInSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uploadUrl;
        private String storagePath;
        private String publicOrDownloadUrl;
        private int expiresInSeconds;

        public Builder uploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; return this; }
        public Builder storagePath(String storagePath) { this.storagePath = storagePath; return this; }
        public Builder publicOrDownloadUrl(String publicOrDownloadUrl) { this.publicOrDownloadUrl = publicOrDownloadUrl; return this; }
        public Builder expiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; return this; }

        public SignedUploadUrlResponse build() {
            return new SignedUploadUrlResponse(uploadUrl, storagePath, publicOrDownloadUrl, expiresInSeconds);
        }
    }

    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getPublicOrDownloadUrl() { return publicOrDownloadUrl; }
    public void setPublicOrDownloadUrl(String publicOrDownloadUrl) { this.publicOrDownloadUrl = publicOrDownloadUrl; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
