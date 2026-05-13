package com.connecthub.websocket.payload;

// ─── TYPING_INDICATOR payload ─────────────────────────────────────────────────
// Sent by client to /app/chat.typing
// Broadcast to /topic/room/{roomId}
public class TypingIndicator {

    private Integer senderId;
    private Integer roomId;
    private String username;
    private Boolean isTyping;

    public TypingIndicator() {}

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getIsTyping() { return isTyping; }
    public void setIsTyping(Boolean isTyping) { this.isTyping = isTyping; }
}
