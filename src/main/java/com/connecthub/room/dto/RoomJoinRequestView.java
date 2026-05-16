package com.connecthub.room.dto;

import java.time.LocalDateTime;

public class RoomJoinRequestView {
    private Integer id;
    private Integer roomId;
    private Integer userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    private String username;
    private String fullName;
    private String avatarUrl;

    public RoomJoinRequestView(Integer id, Integer roomId, Integer userId, String status,
                               LocalDateTime createdAt, LocalDateTime decidedAt,
                               String username, String fullName, String avatarUrl) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    public Integer getId() { return id; }
    public Integer getRoomId() { return roomId; }
    public Integer getUserId() { return userId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
}
