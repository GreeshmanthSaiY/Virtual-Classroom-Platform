package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Answer() {
    }

    public Answer(Long id, Question question, User author, String body, Boolean isAccepted, Instant createdAt) {
        this.id = id;
        this.question = question;
        this.author = author;
        this.body = body;
        this.isAccepted = isAccepted != null ? isAccepted : false;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean getIsAccepted() { return isAccepted; }
    public void setIsAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class Builder {
        private Long id;
        private Question question;
        private User author;
        private String body;
        private Boolean isAccepted = false;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder question(Question question) { this.question = question; return this; }
        public Builder author(User author) { this.author = author; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder isAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Answer build() {
            return new Answer(id, question, author, body, isAccepted, createdAt);
        }
    }
}
