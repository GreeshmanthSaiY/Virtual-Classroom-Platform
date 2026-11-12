package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Question() {}

    public Question(Long id, Subject subject, User author, String title, String body, Boolean isResolved) {
        this.id = id;
        this.subject = subject;
        this.author = author;
        this.title = title;
        this.body = body;
        this.isResolved = isResolved != null ? isResolved : false;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User author;
        private String title;
        private String body;
        private Boolean isResolved = false;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder author(User author) { this.author = author; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder isResolved(Boolean isResolved) { this.isResolved = isResolved; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Question build() {
            Question question = new Question(id, subject, author, title, body, isResolved);
            if (createdAt != null) {
                question.setCreatedAt(createdAt);
            }
            return question;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean isResolved) { this.isResolved = isResolved; }
    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
