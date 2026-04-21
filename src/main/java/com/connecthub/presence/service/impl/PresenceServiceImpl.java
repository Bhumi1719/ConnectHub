package com.connecthub.presence.service.impl;

import com.connecthub.presence.dto.SetOnlineRequest;
import com.connecthub.presence.dto.UpdateStatusRequest;
import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.repository.PresenceRepository;
import com.connecthub.presence.service.PresenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class PresenceServiceImpl implements PresenceService {

    private static final Logger log = Logger.getLogger(PresenceServiceImpl.class.getName());

    private final PresenceRepository presenceRepository;

    @Value("${presence.stale.threshold.seconds:60}")
    private int staleThresholdSeconds;

    public PresenceServiceImpl(PresenceRepository presenceRepository) {
        this.presenceRepository = presenceRepository;
    }

    // ─── Set Online ───────────────────────────────────────────────────────────
    // Called by WebSocket Handler on connection established

    @Override
    public void setOnline(SetOnlineRequest request) {
        LocalDateTime now = LocalDateTime.now();

        // Check if presence record already exists for this user
        Optional<UserPresence> existing = presenceRepository.findByUserId(request.getUserId());

        if (existing.isPresent()) {
            // Update existing record
            UserPresence presence = existing.get();
            presence.setStatus("ONLINE");
            presence.setDeviceType(request.getDeviceType() != null ? request.getDeviceType() : "WEB");
            presence.setIpAddress(request.getIpAddress());
            presence.setSessionId(request.getSessionId());
            presence.setConnectedAt(now);
            presence.setLastPingAt(now);
            presenceRepository.save(presence);
        } else {
            // Create new presence record
            UserPresence presence = UserPresence.builder()
                    .userId(request.getUserId())
                    .status("ONLINE")
                    .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "WEB")
                    .ipAddress(request.getIpAddress())
                    .sessionId(request.getSessionId())
                    .connectedAt(now)
                    .lastPingAt(now)
                    .build();
            presenceRepository.save(presence);
        }

        log.info("User online: userId=" + request.getUserId());
    }

    // ─── Set Offline ──────────────────────────────────────────────────────────
    // Called by WebSocket Handler on connection closed

    @Override
    public void setOffline(Integer userId) {
        presenceRepository.findByUserId(userId).ifPresent(presence -> {
            presence.setStatus("INVISIBLE");
            presence.setSessionId(null);
            presenceRepository.save(presence);
            log.info("User offline: userId=" + userId);
        });
    }

    // ─── Update Status ────────────────────────────────────────────────────────

    @Override
    public void updateStatus(Integer userId, UpdateStatusRequest request) {
        List<String> allowed = List.of("ONLINE", "AWAY", "DND", "INVISIBLE");
        if (!allowed.contains(request.getStatus())) {
            throw new RuntimeException("Invalid status. Allowed: ONLINE, AWAY, DND, INVISIBLE");
        }

        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Presence not found for userId: " + userId));

        presence.setStatus(request.getStatus());
        if (request.getCustomMessage() != null) {
            presence.setCustomMessage(request.getCustomMessage());
        }
        presenceRepository.save(presence);
        log.info("Status updated to " + request.getStatus() + " for userId=" + userId);
    }

    // ─── Get Presence ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPresence> getPresence(Integer userId) {
        return presenceRepository.findByUserId(userId);
    }

    // ─── Bulk Presence ────────────────────────────────────────────────────────
    // Used to load online status for all members in a room at once

    @Override
    @Transactional(readOnly = true)
    public List<UserPresence> getBulkPresence(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new RuntimeException("userIds list cannot be empty");
        }
        return presenceRepository.findByUserIdIn(userIds);
    }

    // ─── Get All Online Users ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UserPresence> getOnlineUsers() {
        return presenceRepository.findOnlineUsers();
    }

    // ─── Ping Session ─────────────────────────────────────────────────────────
    // Called every 30 seconds by WebSocket client to keep session alive

    @Override
    public void pingSession(String sessionId) {
        presenceRepository.findBySessionId(sessionId).ifPresent(presence -> {
            presence.setLastPingAt(LocalDateTime.now());
            presenceRepository.save(presence);
        });
    }

    // ─── Get Online Count ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getOnlineCount() {
        return presenceRepository.findOnlineUsers().size();
    }

    // ─── Clean Stale Sessions ─────────────────────────────────────────────────
    // Runs every 60 seconds automatically
    // If lastPingAt is older than threshold — mark user offline

    @Override
    @Scheduled(fixedDelay = 60000)
    public void cleanStaleSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(staleThresholdSeconds);
        List<UserPresence> staleSessions = presenceRepository.findStaleSessions(threshold);

        if (!staleSessions.isEmpty()) {
            staleSessions.forEach(presence -> {
                presence.setStatus("INVISIBLE");
                presence.setSessionId(null);
                presenceRepository.save(presence);
                log.info("Stale session cleaned for userId=" + presence.getUserId());
            });
            log.info("Cleaned " + staleSessions.size() + " stale sessions");
        }
    }

    // ─── Is Online ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isOnline(Integer userId) {
        return presenceRepository.findByUserId(userId)
                .map(p -> "ONLINE".equals(p.getStatus()))
                .orElse(false);
    }
}
