package com.connecthub.websocket.payload;

// ─── READ_RECEIPT payload ─────────────────────────────────────────────────────
// Sent by client to /app/chat.read
// Broadcast to /topic/room/{roomId}
public class ReadReceipt {

    private Integer readerId;
    private Integer roomId;
    private Integer upToMessageId;

    public ReadReceipt() {}

    public Integer getReaderId() { return readerId; }
    public void setReaderId(Integer readerId) { this.readerId = readerId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public Integer getUpToMessageId() { return upToMessageId; }
    public void setUpToMessageId(Integer upToMessageId) { this.upToMessageId = upToMessageId; }
}
