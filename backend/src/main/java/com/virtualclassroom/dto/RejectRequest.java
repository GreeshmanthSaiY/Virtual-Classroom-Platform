package com.virtualclassroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RejectRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
    @NotBlank(message = "Rejection reason is required")
    private String reason;

    public RejectRequest() {}

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
