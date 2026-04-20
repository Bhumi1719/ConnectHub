package com.connecthub.room.dto;

import jakarta.validation.constraints.NotNull;

public class AddMemberRequest {

    @NotNull(message = "userId is required")
    private Integer userId;

    // ADMIN or MEMBER (default MEMBER)
    private String role = "MEMBER";

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
