package com.connecthub.presence.dto;

import jakarta.validation.constraints.NotNull;

public class SetOnlineRequest {

    @NotNull(message = "userId is required")
    private Integer userId;

    private String deviceType = "WEB";
    private String ipAddress;
    private String sessionId;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
