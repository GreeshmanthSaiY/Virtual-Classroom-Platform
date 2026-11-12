package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload to initiate password reset")
public class ForgotPasswordRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Valid email address must be provided")
    @Schema(description = "User's registered email address", example = "student@virtualclassroom.edu")
    private String email;

    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
