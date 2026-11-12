package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "student_profiles")
public class StudentProfile implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private boolean isNew = true;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Column(name = "roll_number", nullable = false, unique = true, length = 50)
    private String rollNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "current_year", nullable = false)
    private Integer currentYear;

    @Column(columnDefinition = "TEXT")
    private String bio;

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
        private String bio;

        public Builder user(User user) { this.user = user; return this; }
        public Builder rollNumber(String rollNumber) { this.rollNumber = rollNumber; return this; }
        public Builder course(Course course) { this.course = course; return this; }
        public Builder currentYear(Integer currentYear) { this.currentYear = currentYear; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public StudentProfile build() {
            StudentProfile profile = new StudentProfile(user, rollNumber, course, currentYear);
            profile.setBio(bio);
            return profile;
        }
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
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
