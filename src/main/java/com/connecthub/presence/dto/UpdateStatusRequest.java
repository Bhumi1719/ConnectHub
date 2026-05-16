package com.connecthub.presence.dto;

public class UpdateStatusRequest {

    // ONLINE, AWAY, DND, INVISIBLE
    private String status;
    private String customMessage;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
}
