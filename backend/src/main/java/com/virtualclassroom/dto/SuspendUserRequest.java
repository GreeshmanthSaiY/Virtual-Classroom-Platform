package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload to suspend a user account")
public class SuspendUserRequest {

    @NotBlank(message = "Suspension reason must be documented")
    @Schema(description = "Justification for suspending the account", example = "Policy violation under investigation")
    private String reason;

    public SuspendUserRequest() {}

    public SuspendUserRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
