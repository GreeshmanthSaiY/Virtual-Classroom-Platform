package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to change password when authenticated")
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @Schema(description = "Existing password for credential verification", example = "OldSecretPass123!")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
    @Schema(description = "New password complying with complexity requirements", example = "NewSecurePass456!")
    private String newPassword;

    public ChangePasswordRequest() {}

    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
