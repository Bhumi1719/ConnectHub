package com.connecthub.message.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer messageId;

    @Column(nullable = false)
    private Integer roomId;

    @Column(nullable = false)
    private Integer senderId;

    // Message content (text)
    @Column(columnDefinition = "TEXT")
    private String content;

    // TEXT, IMAGE, FILE, REACTION, SYSTEM
    @Column(nullable = false, length = 20)
    private String type = "TEXT";

    // S3 URL for IMAGE or FILE type messages
    @Column(length = 1000)
    private String mediaUrl;

    // For reply/thread — points to parent message
    private Integer replyToMessageId;

    // True if message was edited
    @Column(nullable = false)
    private Boolean isEdited = false;

    // Soft delete — message content hidden but record kept
    @Column(nullable = false)
    private Boolean isDeleted = false;

    // SENT → DELIVERED → READ
    @Column(nullable = false, length = 20)
    private String deliveryStatus = "SENT";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime editedAt;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public Message() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getMessageId() { return messageId; }
    public Integer getRoomId() { return roomId; }
    public Integer getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public String getMediaUrl() { return mediaUrl; }
    public Integer getReplyToMessageId() { return replyToMessageId; }
    public Boolean getIsEdited() { return isEdited; }
    public Boolean getIsDeleted() { return isDeleted; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getEditedAt() { return editedAt; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setReplyToMessageId(Integer replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static MessageBuilder builder() { return new MessageBuilder(); }

    public static class MessageBuilder {
        private Integer roomId;
        private Integer senderId;
        private String content;
        private String type = "TEXT";
        private String mediaUrl;
        private Integer replyToMessageId;
        private String deliveryStatus = "SENT";

        public MessageBuilder roomId(Integer roomId) { this.roomId = roomId; return this; }
        public MessageBuilder senderId(Integer senderId) { this.senderId = senderId; return this; }
        public MessageBuilder content(String content) { this.content = content; return this; }
        public MessageBuilder type(String type) { this.type = type; return this; }
        public MessageBuilder mediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; return this; }
        public MessageBuilder replyToMessageId(Integer replyToMessageId) { this.replyToMessageId = replyToMessageId; return this; }
        public MessageBuilder deliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; return this; }

        public Message build() {
            Message m = new Message();
            m.roomId = this.roomId;
            m.senderId = this.senderId;
            m.content = this.content;
            m.type = this.type;
            m.mediaUrl = this.mediaUrl;
            m.replyToMessageId = this.replyToMessageId;
            m.deliveryStatus = this.deliveryStatus;
            m.isEdited = false;
            m.isDeleted = false;
            return m;
        }
    }

    @Override
    public String toString() {
        return "Message{messageId=" + messageId + ", roomId=" + roomId
                + ", senderId=" + senderId + ", type='" + type + "'}";
    }
}
