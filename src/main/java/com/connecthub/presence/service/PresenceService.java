package com.connecthub.presence.service;

import com.connecthub.presence.dto.SetOnlineRequest;
import com.connecthub.presence.dto.UpdateStatusRequest;
import com.connecthub.presence.entity.UserPresence;

import java.util.List;
import java.util.Optional;

public interface PresenceService {

    // Called when WebSocket connects
    void setOnline(SetOnlineRequest request);

    // Called when WebSocket disconnects
    void setOffline(Integer userId);

    // Update status: ONLINE, AWAY, DND, INVISIBLE
    void updateStatus(Integer userId, UpdateStatusRequest request);

    // Get single user presence
    Optional<UserPresence> getPresence(Integer userId);

    // Get presence for multiple users at once (for room member list)
    List<UserPresence> getBulkPresence(List<Integer> userIds);

    // Get all online users
    List<UserPresence> getOnlineUsers();

    // Ping — update lastPingAt (called every 30 seconds)
    void pingSession(String sessionId);

    // Get total online user count
    int getOnlineCount();

    // Scheduled job — remove stale sessions (no ping in 60 seconds)
    void cleanStaleSessions();

    // Check if a user is online
    boolean isOnline(Integer userId);
}
