package com.connecthub.websocket.payload;

// ─── TYPING_INDICATOR payload ─────────────────────────────────────────────────
// Sent by client to /app/chat.typing
// Broadcast to /topic/room/{roomId}
public class TypingIndicator {

    private Integer senderId;
    private Integer roomId;
    private Boolean isTyping;

    public TypingIndicator() {}

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public Boolean getIsTyping() { return isTyping; }
    public void setIsTyping(Boolean isTyping) { this.isTyping = isTyping; }
}
