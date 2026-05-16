package com.connecthub.room.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roomId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // GROUP or DM
    @Column(nullable = false, length = 10)
    private String type = "GROUP";

    @Column(nullable = false)
    private Integer createdById;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    private Boolean isPrivate = false;

    @Column(nullable = false)
    private Integer maxMembers = 100;

    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public Room() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getRoomId() { return roomId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public Integer getCreatedById() { return createdById; }
    public String getAvatarUrl() { return avatarUrl; }
    public Boolean getIsPrivate() { return isPrivate; }
    public Integer getMaxMembers() { return maxMembers; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static RoomBuilder builder() { return new RoomBuilder(); }

    public static class RoomBuilder {
        private String name;
        private String description;
        private String type = "GROUP";
        private Integer createdById;
        private String avatarUrl;
        private Boolean isPrivate = false;
        private Integer maxMembers = 100;

        public RoomBuilder name(String name) { this.name = name; return this; }
        public RoomBuilder description(String description) { this.description = description; return this; }
        public RoomBuilder type(String type) { this.type = type; return this; }
        public RoomBuilder createdById(Integer createdById) { this.createdById = createdById; return this; }
        public RoomBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public RoomBuilder isPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; return this; }
        public RoomBuilder maxMembers(Integer maxMembers) { this.maxMembers = maxMembers; return this; }

        public Room build() {
            Room r = new Room();
            r.name = this.name;
            r.description = this.description;
            r.type = this.type;
            r.createdById = this.createdById;
            r.avatarUrl = this.avatarUrl;
            r.isPrivate = this.isPrivate;
            r.maxMembers = this.maxMembers;
            return r;
        }
    }

    @Override
    public String toString() {
        return "Room{roomId=" + roomId + ", name='" + name + "', type='" + type + "'}";
    }
}
