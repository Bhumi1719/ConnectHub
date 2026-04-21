package com.connecthub.message.dto;

public class UpdateDeliveryStatusRequest {

    // SENT, DELIVERED, READ
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
