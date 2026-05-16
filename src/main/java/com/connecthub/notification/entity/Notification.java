package com.connecthub.notification.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    // Who receives this notification
    @Column(nullable = false)
    private Integer recipientId;

    // Who triggered this notification (sender/admin)
    private Integer actorId;

    // NEW_MESSAGE, MENTION, ROOM_INVITE, SYSTEM
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    // Optional — which room this notification is about
    private Integer roomId;

    // Optional — which message this notification is about
    private Integer messageId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public Notification() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getNotificationId() { return notificationId; }
    public Integer getRecipientId() { return recipientId; }
    public Integer getActorId() { return actorId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Integer getRoomId() { return roomId; }
    public Integer getMessageId() { return messageId; }
    public Boolean getIsRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }
    public void setRecipientId(Integer recipientId) { this.recipientId = recipientId; }
    public void setActorId(Integer actorId) { this.actorId = actorId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static NotificationBuilder builder() { return new NotificationBuilder(); }

    public static class NotificationBuilder {
        private Integer recipientId;
        private Integer actorId;
        private String type;
        private String title;
        private String message;
        private Integer roomId;
        private Integer messageId;
        private Boolean isRead = false;

        public NotificationBuilder recipientId(Integer recipientId) { this.recipientId = recipientId; return this; }
        public NotificationBuilder actorId(Integer actorId) { this.actorId = actorId; return this; }
        public NotificationBuilder type(String type) { this.type = type; return this; }
        public NotificationBuilder title(String title) { this.title = title; return this; }
        public NotificationBuilder message(String message) { this.message = message; return this; }
        public NotificationBuilder roomId(Integer roomId) { this.roomId = roomId; return this; }
        public NotificationBuilder messageId(Integer messageId) { this.messageId = messageId; return this; }

        public Notification build() {
            Notification n = new Notification();
            n.recipientId = this.recipientId;
            n.actorId = this.actorId;
            n.type = this.type;
            n.title = this.title;
            n.message = this.message;
            n.roomId = this.roomId;
            n.messageId = this.messageId;
            n.isRead = this.isRead;
            return n;
        }
    }

    @Override
    public String toString() {
        return "Notification{id=" + notificationId + ", recipientId=" + recipientId
                + ", type='" + type + "', isRead=" + isRead + "}";
    }
}
