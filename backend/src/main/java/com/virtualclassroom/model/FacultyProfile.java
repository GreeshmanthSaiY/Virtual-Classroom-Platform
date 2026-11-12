package com.virtualclassroom.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculty_profiles")
public class FacultyProfile implements Persistable<Long> {

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

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 100)
    private String designation = "Faculty Instructor";

    @Column(name = "office_hours", length = 100)
    private String officeHours;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @OneToMany(mappedBy = "facultyProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacultySubjectAssignment> subjectAssignments = new ArrayList<>();

    public FacultyProfile() {}

    public FacultyProfile(User user, String employeeId, String department) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.employeeId = employeeId;
        this.department = department;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private User user;
        private String employeeId;
        private String department;
        private String designation = "Faculty Instructor";
        private String officeHours;
        private String bio;

        public Builder user(User user) { this.user = user; return this; }
        public Builder employeeId(String employeeId) { this.employeeId = employeeId; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder officeHours(String officeHours) { this.officeHours = officeHours; return this; }
        public Builder bio(String bio) { this.bio = bio; return this; }
        public FacultyProfile build() {
            FacultyProfile profile = new FacultyProfile(user, employeeId, department);
            profile.setDesignation(designation);
            profile.setOfficeHours(officeHours);
            profile.setBio(bio);
            return profile;
        }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; this.userId = user != null ? user.getId() : null; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getOfficeHours() { return officeHours; }
    public void setOfficeHours(String officeHours) { this.officeHours = officeHours; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public List<FacultySubjectAssignment> getSubjectAssignments() { return subjectAssignments; }
    public void setSubjectAssignments(List<FacultySubjectAssignment> subjectAssignments) { this.subjectAssignments = subjectAssignments; }
}
