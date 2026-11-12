package com.virtualclassroom.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "year_level", nullable = false)
    private Integer yearLevel;

    @Column(nullable = false)
    private Integer semester = 1;

    @Column(nullable = false)
    private Integer credits = 3;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Subject() {}

    public Subject(Integer id, Course course, String code, String title, Integer yearLevel) {
        this.id = id;
        this.course = course;
        this.code = code;
        this.title = title;
        this.yearLevel = yearLevel;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private Course course;
        private String code;
        private String title;
        private Integer yearLevel;
        private Integer semester = 1;
        private Integer credits = 3;
        private String description;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder course(Course course) { this.course = course; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder yearLevel(Integer yearLevel) { this.yearLevel = yearLevel; return this; }
        public Builder semester(Integer semester) { this.semester = semester; return this; }
        public Builder credits(Integer credits) { this.credits = credits; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Subject build() {
            Subject subject = new Subject(id, course, code, title, yearLevel);
            subject.setSemester(semester != null ? semester : 1);
            subject.setCredits(credits != null ? credits : 3);
            subject.setDescription(description);
            return subject;
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getYearLevel() { return yearLevel; }
    public void setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
