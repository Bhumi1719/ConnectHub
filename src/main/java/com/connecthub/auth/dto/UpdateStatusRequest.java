package com.connecthub.auth.dto;

import lombok.Data;

@Data
public class UpdateStatusRequest {
    // ONLINE, AWAY, DND, INVISIBLE
    private String status;
}
