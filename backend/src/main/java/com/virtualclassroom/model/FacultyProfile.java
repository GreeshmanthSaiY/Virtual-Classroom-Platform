package com.virtualclassroom.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculty_profiles")
public class FacultyProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @Column(nullable = false, length = 100)
    private String department;

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

        public Builder user(User user) { this.user = user; return this; }
        public Builder employeeId(String employeeId) { this.employeeId = employeeId; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public FacultyProfile build() { return new FacultyProfile(user, employeeId, department); }
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; this.userId = user != null ? user.getId() : null; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<FacultySubjectAssignment> getSubjectAssignments() { return subjectAssignments; }
    public void setSubjectAssignments(List<FacultySubjectAssignment> subjectAssignments) { this.subjectAssignments = subjectAssignments; }
}
