package com.connecthub.presence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
public class UserPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer presenceId;

    @Column(nullable = false, unique = true)
    private Integer userId;

    // ONLINE, AWAY, DND, INVISIBLE
    @Column(nullable = false, length = 20)
    private String status = "ONLINE";

    @Column(length = 100)
    private String customMessage;

    // WEB, MOBILE, DESKTOP
    @Column(length = 20)
    private String deviceType = "WEB";

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    // Updated every 30 seconds by WebSocket ping
    @Column(nullable = false)
    private LocalDateTime lastPingAt;

    // Unique WebSocket session ID
    @Column(length = 100)
    private String sessionId;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public UserPresence() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getPresenceId() { return presenceId; }
    public Integer getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getCustomMessage() { return customMessage; }
    public String getDeviceType() { return deviceType; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public LocalDateTime getLastPingAt() { return lastPingAt; }
    public String getSessionId() { return sessionId; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setPresenceId(Integer presenceId) { this.presenceId = presenceId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public void setStatus(String status) { this.status = status; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
    public void setLastPingAt(LocalDateTime lastPingAt) { this.lastPingAt = lastPingAt; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static UserPresenceBuilder builder() { return new UserPresenceBuilder(); }

    public static class UserPresenceBuilder {
        private Integer userId;
        private String status = "ONLINE";
        private String customMessage;
        private String deviceType = "WEB";
        private String ipAddress;
        private LocalDateTime connectedAt;
        private LocalDateTime lastPingAt;
        private String sessionId;

        public UserPresenceBuilder userId(Integer userId) { this.userId = userId; return this; }
        public UserPresenceBuilder status(String status) { this.status = status; return this; }
        public UserPresenceBuilder customMessage(String customMessage) { this.customMessage = customMessage; return this; }
        public UserPresenceBuilder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public UserPresenceBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public UserPresenceBuilder connectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; return this; }
        public UserPresenceBuilder lastPingAt(LocalDateTime lastPingAt) { this.lastPingAt = lastPingAt; return this; }
        public UserPresenceBuilder sessionId(String sessionId) { this.sessionId = sessionId; return this; }

        public UserPresence build() {
            UserPresence p = new UserPresence();
            p.userId = this.userId;
            p.status = this.status;
            p.customMessage = this.customMessage;
            p.deviceType = this.deviceType;
            p.ipAddress = this.ipAddress;
            p.connectedAt = this.connectedAt;
            p.lastPingAt = this.lastPingAt;
            p.sessionId = this.sessionId;
            return p;
        }
    }

    @Override
    public String toString() {
        return "UserPresence{userId=" + userId + ", status='" + status + "', sessionId='" + sessionId + "'}";
    }
}
