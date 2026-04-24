package com.connecthub.websocket.payload;

// ─── REACTION payload ─────────────────────────────────────────────────────────
public class Reaction {
    private Integer senderId;
    private Integer messageId;
    private Integer roomId;
    private String emoji;

    public Reaction() {}

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}
