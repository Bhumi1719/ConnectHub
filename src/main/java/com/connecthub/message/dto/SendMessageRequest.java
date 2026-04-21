package com.connecthub.message.dto;

import jakarta.validation.constraints.NotNull;

public class SendMessageRequest {

    @NotNull(message = "roomId is required")
    private Integer roomId;

    @NotNull(message = "senderId is required")
    private Integer senderId;

    private String content;

    // TEXT, IMAGE, FILE, REACTION, SYSTEM
    private String type = "TEXT";

    private String mediaUrl;

    // For reply — set this to parent messageId
    private Integer replyToMessageId;

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public Integer getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Integer replyToMessageId) { this.replyToMessageId = replyToMessageId; }
}
