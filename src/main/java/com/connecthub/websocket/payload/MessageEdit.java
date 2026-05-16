package com.connecthub.websocket.payload;

// ─── MESSAGE_EDIT payload ─────────────────────────────────────────────────────
public class MessageEdit {
    private Integer editorId;
    private Integer messageId;
    private Integer roomId;
    private String newContent;

    public MessageEdit() {}

    public Integer getEditorId() { return editorId; }
    public void setEditorId(Integer editorId) { this.editorId = editorId; }
    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getNewContent() { return newContent; }
    public void setNewContent(String newContent) { this.newContent = newContent; }
}
