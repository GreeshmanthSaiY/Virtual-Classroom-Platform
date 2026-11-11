package com.virtualclassroom.dto;


import java.time.Instant;

public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private Instant createdAt;

    public NotificationDto() {}

    public NotificationDto(Long id, String title, String message, String type, Boolean isRead, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean isRead() { return isRead; }
    public void setRead(Boolean isRead) { this.isRead = isRead; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String message;
        private String type;
        private Boolean isRead;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        public Builder isRead(Boolean isRead) {
            this.isRead = isRead;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationDto build() {
            return new NotificationDto(id, title, message, type, isRead, createdAt);
        }
    }
}
