package com.connecthub.message.dto;

import jakarta.validation.constraints.NotBlank;

public class EditMessageRequest {

    @NotBlank(message = "content is required")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
