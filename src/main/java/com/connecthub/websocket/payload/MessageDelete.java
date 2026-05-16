package com.connecthub.websocket.payload;

// ─── MESSAGE_DELETE payload ───────────────────────────────────────────────────
public class MessageDelete {
    private Integer deleterId;
    private Integer messageId;
    private Integer roomId;

    public MessageDelete() {}

    public Integer getDeleterId() { return deleterId; }
    public void setDeleterId(Integer deleterId) { this.deleterId = deleterId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
}
