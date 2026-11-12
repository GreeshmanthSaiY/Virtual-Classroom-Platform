package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "examinations")
public class Examination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 30)
    private ExamType examType = ExamType.MIDTERM;

    @Column(name = "total_marks", nullable = false)
    private Integer totalMarks = 100;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @OneToMany(mappedBy = "examination", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExaminationResult> results = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Examination() {}

    public Examination(Subject subject, User creator, String title, ExamType examType, Integer totalMarks,
                       Integer durationMinutes, Instant scheduledAt, String instructions) {
        this.subject = subject;
        this.creator = creator;
        this.title = title;
        this.examType = examType != null ? examType : ExamType.MIDTERM;
        this.totalMarks = totalMarks != null ? totalMarks : 100;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 60;
        this.scheduledAt = scheduledAt;
        this.instructions = instructions;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Subject subject;
        private User creator;
        private String title;
        private ExamType examType = ExamType.MIDTERM;
        private Integer totalMarks = 100;
        private Integer durationMinutes = 60;
        private Instant scheduledAt;
        private String instructions;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        public Builder creator(User creator) { this.creator = creator; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder examType(ExamType examType) { this.examType = examType; return this; }
        public Builder totalMarks(Integer totalMarks) { this.totalMarks = totalMarks; return this; }
        public Builder durationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder scheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; return this; }
        public Builder instructions(String instructions) { this.instructions = instructions; return this; }
        public Examination build() {
            Examination examination = new Examination(subject, creator, title, examType, totalMarks, durationMinutes, scheduledAt, instructions);
            examination.setId(id);
            return examination;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ExamType getExamType() { return examType; }
    public void setExamType(ExamType examType) { this.examType = examType; }
    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public List<ExaminationResult> getResults() { return results; }
    public void setResults(List<ExaminationResult> results) { this.results = results; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

