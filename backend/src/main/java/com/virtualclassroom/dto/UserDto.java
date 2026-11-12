package com.virtualclassroom.dto;


import java.time.Instant;

public class UserDto {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String rejectionReason;
    private String rollNumber;
    private String employeeId;
    private String department;
    private String courseName;
    private Integer currentYear;
    private String avatarUrl;
    private String phoneNumber;
    private Instant createdAt;

    public UserDto() {}

    public UserDto(Long id, String email, String fullName, String role, String status, String rejectionReason, String rollNumber, String employeeId, String department, String courseName, Integer currentYear, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.rollNumber = rollNumber;
        this.employeeId = employeeId;
        this.department = department;
        this.courseName = courseName;
        this.currentYear = currentYear;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public Integer getCurrentYear() { return currentYear; }
    public void setCurrentYear(Integer currentYear) { this.currentYear = currentYear; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static UserDtoBuilder builder() {
        return new UserDtoBuilder();
    }

    public static class UserDtoBuilder {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private String status;
        private String rejectionReason;
        private String rollNumber;
        private String employeeId;
        private String department;
        private String courseName;
        private Integer currentYear;
        private String avatarUrl;
        private String phoneNumber;
        private Instant createdAt;

        public UserDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public UserDtoBuilder email(String email) {
            this.email = email;
            return this;
        }
        public UserDtoBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }
        public UserDtoBuilder role(String role) {
            this.role = role;
            return this;
        }
        public UserDtoBuilder status(String status) {
            this.status = status;
            return this;
        }
        public UserDtoBuilder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }
        public UserDtoBuilder rollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
            return this;
        }
        public UserDtoBuilder employeeId(String employeeId) {
            this.employeeId = employeeId;
            return this;
        }
        public UserDtoBuilder department(String department) {
            this.department = department;
            return this;
        }
        public UserDtoBuilder courseName(String courseName) {
            this.courseName = courseName;
            return this;
        }
        public UserDtoBuilder currentYear(Integer currentYear) {
            this.currentYear = currentYear;
            return this;
        }
        public UserDtoBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }
        public UserDtoBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }
        public UserDtoBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDto build() {
            UserDto dto = new UserDto(id, email, fullName, role, status, rejectionReason, rollNumber, employeeId, department, courseName, currentYear, createdAt);
            dto.setAvatarUrl(avatarUrl);
            dto.setPhoneNumber(phoneNumber);
            return dto;
        }
    }

    public static class Builder extends UserDtoBuilder {}
}
