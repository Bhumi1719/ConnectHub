package com.connecthub.notification.dto;

import java.util.List;

public class BulkNotificationRequest {

    private List<Integer> recipientIds;
    private Integer actorId;
    private String type;
    private String title;
    private String message;
    private Integer roomId;

    public List<Integer> getRecipientIds() { return recipientIds; }
    public void setRecipientIds(List<Integer> recipientIds) { this.recipientIds = recipientIds; }
    public Integer getActorId() { return actorId; }
    public void setActorId(Integer actorId) { this.actorId = actorId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
}
