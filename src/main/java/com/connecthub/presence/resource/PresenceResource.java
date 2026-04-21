package com.connecthub.presence.resource;

import com.connecthub.presence.dto.BulkPresenceRequest;
import com.connecthub.presence.dto.SetOnlineRequest;
import com.connecthub.presence.dto.UpdateStatusRequest;
import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.service.PresenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/presence")
public class PresenceResource {

    private final PresenceService presenceService;

    public PresenceResource(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    // ─── POST /presence/online ────────────────────────────────────────────────
    // Called by WebSocket Handler when user connects
    @PostMapping("/online")
    public ResponseEntity<Map<String, String>> setOnline(
            @Valid @RequestBody SetOnlineRequest request) {
        presenceService.setOnline(request);
        return ResponseEntity.ok(Map.of("message", "User is now ONLINE"));
    }

    // ─── PUT /presence/offline/{userId} ──────────────────────────────────────
    // Called by WebSocket Handler when user disconnects
    @PutMapping("/offline/{userId}")
    public ResponseEntity<Map<String, String>> setOffline(@PathVariable Integer userId) {
        presenceService.setOffline(userId);
        return ResponseEntity.ok(Map.of("message", "User is now OFFLINE"));
    }

    // ─── PUT /presence/status/{userId} ───────────────────────────────────────
    // User manually changes their status
    @PutMapping("/status/{userId}")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Integer userId,
            @RequestBody UpdateStatusRequest request) {
        presenceService.updateStatus(userId, request);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
    }

    // ─── GET /presence/{userId} ───────────────────────────────────────────────
    // Get single user presence
    @GetMapping("/{userId}")
    public ResponseEntity<?> getPresence(@PathVariable Integer userId) {
        Optional<UserPresence> presence = presenceService.getPresence(userId);
        if (presence.isPresent()) {
            return ResponseEntity.ok(presence.get());
        }
        return ResponseEntity.ok(Map.of("userId", userId, "status", "OFFLINE"));
    }

    // ─── POST /presence/bulk ─────────────────────────────────────────────────
    // Get presence for multiple users (room member list)
    @PostMapping("/bulk")
    public ResponseEntity<List<UserPresence>> getBulkPresence(
            @RequestBody BulkPresenceRequest request) {
        List<UserPresence> presenceList = presenceService.getBulkPresence(request.getUserIds());
        return ResponseEntity.ok(presenceList);
    }

    // ─── GET /presence/online/all ─────────────────────────────────────────────
    // Get all online users
    @GetMapping("/online/all")
    public ResponseEntity<List<UserPresence>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    // ─── GET /presence/online/count ───────────────────────────────────────────
    // Get total online user count (for admin dashboard)
    @GetMapping("/online/count")
    public ResponseEntity<Map<String, Integer>> getOnlineCount() {
        return ResponseEntity.ok(Map.of("onlineCount", presenceService.getOnlineCount()));
    }

    // ─── PUT /presence/ping/{sessionId} ──────────────────────────────────────
    // Heartbeat — called every 30 seconds to keep session alive
    @PutMapping("/ping/{sessionId}")
    public ResponseEntity<Map<String, String>> ping(@PathVariable String sessionId) {
        presenceService.pingSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Ping received"));
    }

    // ─── GET /presence/isonline/{userId} ─────────────────────────────────────
    // Quick check — is user online?
    @GetMapping("/isonline/{userId}")
    public ResponseEntity<Map<String, Boolean>> isOnline(@PathVariable Integer userId) {
        boolean online = presenceService.isOnline(userId);
        return ResponseEntity.ok(Map.of("isOnline", online));
    }
}
