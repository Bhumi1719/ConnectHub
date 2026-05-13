package com.connecthub.room.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_members",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"room_id", "user_id"})
       })
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer memberId;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // ADMIN or MEMBER
    @Column(nullable = false, length = 10)
    private String role = "MEMBER";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // Used to calculate unread message count
    private LocalDateTime lastReadAt;

    // Muted members can read but cannot send messages
    @Column(nullable = false)
    private Boolean isMuted = false;

    @Transient
    private String username;

    @Transient
    private String email;

    @Transient
    private String fullName;

    @Transient
    private String avatarUrl;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public RoomMember() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getMemberId() { return memberId; }
    public Integer getRoomId() { return roomId; }
    public Integer getUserId() { return userId; }
    public String getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public Boolean getIsMuted() { return isMuted; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setRole(String role) { this.role = role; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
    public void setIsMuted(Boolean isMuted) { this.isMuted = isMuted; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static RoomMemberBuilder builder() { return new RoomMemberBuilder(); }

    public static class RoomMemberBuilder {
        private Integer roomId;
        private Integer userId;
        private String role = "MEMBER";
        private Boolean isMuted = false;

        public RoomMemberBuilder roomId(Integer roomId) { this.roomId = roomId; return this; }
        public RoomMemberBuilder userId(Integer userId) { this.userId = userId; return this; }
        public RoomMemberBuilder role(String role) { this.role = role; return this; }
        public RoomMemberBuilder isMuted(Boolean isMuted) { this.isMuted = isMuted; return this; }

        public RoomMember build() {
            RoomMember rm = new RoomMember();
            rm.roomId = this.roomId;
            rm.userId = this.userId;
            rm.role = this.role;
            rm.isMuted = this.isMuted;
            return rm;
        }
    }

    @Override
    public String toString() {
        return "RoomMember{memberId=" + memberId + ", roomId=" + roomId + ", userId=" + userId + ", role='" + role + "'}";
    }
}
