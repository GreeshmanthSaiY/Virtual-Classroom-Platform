package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Storage file upload metadata response")
public class FileUploadResponse {

    private String fileUrl;
    private String storagePath;
    private String originalFilename;
    private String mimeType;
    private long fileSizeBytes;

    public FileUploadResponse() {}

    public FileUploadResponse(String fileUrl, String storagePath, String originalFilename, String mimeType, long fileSizeBytes) {
        this.fileUrl = fileUrl;
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fileUrl;
        private String storagePath;
        private String originalFilename;
        private String mimeType;
        private long fileSizeBytes;

        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder storagePath(String storagePath) { this.storagePath = storagePath; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder fileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }

        public FileUploadResponse build() {
            return new FileUploadResponse(fileUrl, storagePath, originalFilename, mimeType, fileSizeBytes);
        }
    }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
}
