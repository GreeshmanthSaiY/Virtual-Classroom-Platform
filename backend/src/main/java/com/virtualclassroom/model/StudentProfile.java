package com.virtualclassroom.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "roll_number", nullable = false, unique = true, length = 50)
    private String rollNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "current_year", nullable = false)
    private Integer currentYear;

    public StudentProfile() {}

    public StudentProfile(User user, String rollNumber, Course course, Integer currentYear) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.rollNumber = rollNumber;
        this.course = course;
        this.currentYear = currentYear;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private String rollNumber;
        private Course course;
        private Integer currentYear;

        public Builder user(User user) { this.user = user; return this; }
        public Builder rollNumber(String rollNumber) { this.rollNumber = rollNumber; return this; }
        public Builder course(Course course) { this.course = course; return this; }
        public Builder currentYear(Integer currentYear) { this.currentYear = currentYear; return this; }
        public StudentProfile build() { return new StudentProfile(user, rollNumber, course, currentYear); }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; this.userId = user != null ? user.getId() : null; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }
}
