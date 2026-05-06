package com.connecthub.room.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_join_requests",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"room_id", "user_id", "status"})
       })
public class RoomJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    public Integer getId() { return id; }
    public Integer getRoomId() { return roomId; }
    public Integer getUserId() { return userId; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }

    public void setId(Integer id) { this.id = id; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setStatus(String status) { this.status = status; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
