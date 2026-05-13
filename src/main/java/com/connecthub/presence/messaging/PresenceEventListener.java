package com.connecthub.presence.messaging;

import com.connecthub.presence.config.RabbitMQConfig;
import com.connecthub.presence.dto.SetOnlineRequest;
import com.connecthub.presence.dto.UpdateStatusRequest;
import com.connecthub.presence.service.PresenceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Consumes presence events published by websocket-service and updates
 * the presence database accordingly.
 *
 * Replaces the previous pattern where websocket-service called
 * presence-service REST endpoints directly on every connect/disconnect.
 *
 * Queues consumed:
 *   presence.db.online   → set user ONLINE in DB
 *   presence.db.offline  → set user INVISIBLE in DB
 *   presence.db.update   → update user status in DB
 */
@Component
public class PresenceEventListener {

    private static final Logger log = Logger.getLogger(PresenceEventListener.class.getName());

    private final PresenceService presenceService;

    public PresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /**
     * Consumed from: presence.db.online
     * Set user presence to ONLINE in the database.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRESENCE_DB_ONLINE)
    public void handleUserOnline(Map<String, Object> event) {
        log.info("[RabbitMQ] Received presence.online for userId=" + event.get("userId"));
        try {
            SetOnlineRequest request = new SetOnlineRequest();
            request.setUserId(toInt(event.get("userId")));
            request.setSessionId((String) event.get("sessionId"));
            request.setDeviceType((String) event.getOrDefault("deviceType", "WEB"));
            request.setIpAddress((String) event.get("ipAddress"));

            presenceService.setOnline(request);
            log.info("[RabbitMQ] User marked ONLINE: userId=" + request.getUserId());

        } catch (Exception e) {
            log.warning("[RabbitMQ] Failed to set user online: " + e.getMessage());
        }
    }

    /**
     * Consumed from: presence.db.offline
     * Set user presence to INVISIBLE (offline) in the database.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRESENCE_DB_OFFLINE)
    public void handleUserOffline(Map<String, Object> event) {
        log.info("[RabbitMQ] Received presence.offline for userId=" + event.get("userId"));
        try {
            Integer userId = toInt(event.get("userId"));
            presenceService.setOffline(userId);
            log.info("[RabbitMQ] User marked OFFLINE: userId=" + userId);

        } catch (Exception e) {
            log.warning("[RabbitMQ] Failed to set user offline: " + e.getMessage());
        }
    }

    /**
     * Consumed from: presence.db.update
     * Update user status (ONLINE, AWAY, DND, INVISIBLE) in the database.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_PRESENCE_DB_UPDATE)
    public void handlePresenceUpdate(Map<String, Object> event) {
        log.info("[RabbitMQ] Received presence.update for userId=" + event.get("userId")
                + " status=" + event.get("status"));
        try {
            Integer userId = toInt(event.get("userId"));
            String  status = (String) event.get("status");

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(status);

            presenceService.updateStatus(userId, request);
            log.info("[RabbitMQ] Presence updated for userId=" + userId + " → " + status);

        } catch (Exception e) {
            log.warning("[RabbitMQ] Failed to update presence: " + e.getMessage());
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.valueOf(value.toString());
    }
}
