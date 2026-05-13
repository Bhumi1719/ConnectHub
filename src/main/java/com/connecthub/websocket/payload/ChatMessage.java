package com.connecthub.websocket.payload;

// ─── CHAT_MESSAGE payload ─────────────────────────────────────────────────────
// Sent by client to /app/chat.send
// Broadcast to /topic/room/{roomId}
public class ChatMessage {

    private Integer messageId;
    private Integer senderId;
    private Integer roomId;
    private Integer recipientId;
    private String senderUsername;
    private String content;
    // TEXT, IMAGE, FILE, REACTION, SYSTEM
    private String type = "TEXT";
    private String mediaUrl;
    private Integer replyToMessageId;
    private String sentAt;
    private String createdAt;

    public ChatMessage() {}

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public Integer getRecipientId() { return recipientId; }
    public void setRecipientId(Integer recipientId) { this.recipientId = recipientId; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public Integer getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Integer replyToMessageId) { this.replyToMessageId = replyToMessageId; }
    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
