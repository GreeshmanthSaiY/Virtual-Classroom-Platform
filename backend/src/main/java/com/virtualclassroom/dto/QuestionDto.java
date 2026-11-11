package com.virtualclassroom.dto;


import java.time.Instant;
import java.util.List;

public class QuestionDto {

    private Long id;
    private Integer subjectId;
    private String subjectTitle;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String title;
    private String body;
    private Boolean isResolved;
    private Integer answersCount;
    private List<AnswerDto> answers;
    private Instant createdAt;

    public QuestionDto() {}

    public QuestionDto(Long id, Integer subjectId, String subjectTitle, Long authorId, String authorName, String authorRole, String title, String body, Boolean isResolved, Integer answersCount, List<AnswerDto> answers, Instant createdAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectTitle = subjectTitle;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.title = title;
        this.body = body;
        this.isResolved = isResolved;
        this.answersCount = answersCount;
        this.answers = answers;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectTitle() { return subjectTitle; }
    public void setSubjectTitle(String subjectTitle) { this.subjectTitle = subjectTitle; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean isResolved() { return isResolved; }
    public void setResolved(Boolean isResolved) { this.isResolved = isResolved; }
    public Integer getAnswersCount() { return answersCount; }
    public void setAnswersCount(Integer answersCount) { this.answersCount = answersCount; }
    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Integer subjectId;
        private String subjectTitle;
        private Long authorId;
        private String authorName;
        private String authorRole;
        private String title;
        private String body;
        private Boolean isResolved;
        private Integer answersCount;
        private List<AnswerDto> answers;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Builder subjectId(Integer subjectId) {
            this.subjectId = subjectId;
            return this;
        }
        public Builder subjectTitle(String subjectTitle) {
            this.subjectTitle = subjectTitle;
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
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        public Builder isResolved(Boolean isResolved) {
            this.isResolved = isResolved;
            return this;
        }
        public Builder answersCount(Integer answersCount) {
            this.answersCount = answersCount;
            return this;
        }
        public Builder answers(List<AnswerDto> answers) {
            this.answers = answers;
            return this;
        }
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public QuestionDto build() {
            return new QuestionDto(id, subjectId, subjectTitle, authorId, authorName, authorRole, title, body, isResolved, answersCount, answers, createdAt);
        }
    }
}
