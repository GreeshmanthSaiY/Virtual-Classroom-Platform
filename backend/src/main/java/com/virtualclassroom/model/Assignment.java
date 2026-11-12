package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "max_points", nullable = false)
    private Integer maxPoints = 100;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssignmentSubmission> submissions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public Assignment() {}

    public Assignment(Subject subject, User faculty, String title, String description, Instant dueDate, Integer maxPoints, String attachmentUrl) {
        this.subject = subject;
        this.faculty = faculty;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxPoints = maxPoints != null ? maxPoints : 100;
        this.attachmentUrl = attachmentUrl;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User faculty;
        private String title;
        private String description;
        private Instant dueDate;
        private Integer maxPoints = 100;
        private String attachmentUrl;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder faculty(User faculty) { this.faculty = faculty; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder dueDate(Instant dueDate) { this.dueDate = dueDate; return this; }
        public Builder maxPoints(Integer maxPoints) { this.maxPoints = maxPoints; return this; }
        public Builder attachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; return this; }
        public Assignment build() {
            Assignment assignment = new Assignment(subject, faculty, title, description, dueDate, maxPoints, attachmentUrl);
            assignment.setId(id);
            return assignment;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public List<AssignmentSubmission> getSubmissions() { return submissions; }
    public void setSubmissions(List<AssignmentSubmission> submissions) { this.submissions = submissions; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

