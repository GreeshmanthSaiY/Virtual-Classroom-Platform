package com.virtualclassroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RegisterFacultyRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    @NotBlank(message = "Department is required")
    private String department;
    @NotNull(message = "At least one subject must be specified")
    private List<Integer> initialSubjectIds;

    public RegisterFacultyRequest() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<Integer> getInitialSubjectIds() { return initialSubjectIds; }
    public void setInitialSubjectIds(List<Integer> initialSubjectIds) { this.initialSubjectIds = initialSubjectIds; }
}
