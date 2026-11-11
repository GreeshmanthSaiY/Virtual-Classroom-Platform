package com.virtualclassroom.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ApprovalRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;

    public ApprovalRequest() {}

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
}
