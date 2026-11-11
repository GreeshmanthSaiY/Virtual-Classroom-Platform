package com.virtualclassroom.dto;


import java.time.Instant;

public class AnswerDto {

    private Long id;
    private Long questionId;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String body;
    private Boolean isAccepted;
    private Instant createdAt;

    public AnswerDto() {}

    public AnswerDto(Long id, Long questionId, Long authorId, String authorName, String authorRole, String body, Boolean isAccepted, Instant createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.body = body;
        this.isAccepted = isAccepted;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean isAccepted() { return isAccepted; }
    public void setAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long questionId;
        private Long authorId;
        private String authorName;
        private String authorRole;
        private String body;
        private Boolean isAccepted;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Builder questionId(Long questionId) {
            this.questionId = questionId;
            return this;
        }
        public Builder authorId(Long authorId) {
            this.authorId = authorId;
            return this;
        }
        public Builder authorName(String authorName) {
            this.authorName = authorName;
            return this;
        }
        public Builder authorRole(String authorRole) {
            this.authorRole = authorRole;
            return this;
        }
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        public Builder isAccepted(Boolean isAccepted) {
            this.isAccepted = isAccepted;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AnswerDto build() {
            return new AnswerDto(id, questionId, authorId, authorName, authorRole, body, isAccepted, createdAt);
        }
    }
}
