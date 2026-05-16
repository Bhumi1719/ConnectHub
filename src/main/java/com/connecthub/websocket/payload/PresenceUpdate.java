package com.connecthub.websocket.payload;

// ─── PRESENCE_UPDATE payload ──────────────────────────────────────────────────
// Broadcast to /topic/presence when user connects/disconnects/changes status
public class PresenceUpdate {

    private Integer userId;
    // ONLINE, AWAY, DND, INVISIBLE
    private String status;
    private String customMessage;

    public PresenceUpdate() {}

    public PresenceUpdate(Integer userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
}
