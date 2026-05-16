package com.connecthub.websocket.messaging;

import com.connecthub.websocket.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Published from websocket-service when a WebSocket connection is established
 * or disconnected.
 *
 * Events published:
 *   presence.online  → presence-service updates DB, ws-service broadcasts via STOMP
 *   presence.offline → presence-service updates DB, ws-service broadcasts via STOMP
 *   presence.update  → user manually changed their status
 */
@Component
public class PresenceEventPublisher {

    private static final Logger log = Logger.getLogger(PresenceEventPublisher.class.getName());

    private final RabbitTemplate rabbitTemplate;

    public PresenceEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Published when a WebSocket session connects.
     * Presence-service will set user ONLINE.
     */
    public void publishUserOnline(Integer userId, String sessionId,
                                   String deviceType, String ipAddress) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_ONLINE");
        event.put("userId", userId);
        event.put("sessionId", sessionId);
        event.put("deviceType", deviceType != null ? deviceType : "WEB");
        event.put("ipAddress", ipAddress);
        event.put("status", "ONLINE");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRESENCE_EXCHANGE,
                RabbitMQConfig.ROUTING_PRESENCE_ONLINE,
                event
        );
        log.info("[RabbitMQ] Published presence.online for userId=" + userId);
    }

    /**
     * Published when a WebSocket session disconnects.
     * Presence-service will set user INVISIBLE.
     */
    public void publishUserOffline(Integer userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "USER_OFFLINE");
        event.put("userId", userId);
        event.put("status", "INVISIBLE");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRESENCE_EXCHANGE,
                RabbitMQConfig.ROUTING_PRESENCE_OFFLINE,
                event
        );
        log.info("[RabbitMQ] Published presence.offline for userId=" + userId);
    }

    /**
     * Published when user manually changes their status via /app/presence.update.
     */
    public void publishPresenceUpdate(Integer userId, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "PRESENCE_UPDATE");
        event.put("userId", userId);
        event.put("status", status);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRESENCE_EXCHANGE,
                RabbitMQConfig.ROUTING_PRESENCE_UPDATE,
                event
        );
        log.info("[RabbitMQ] Published presence.update for userId=" + userId
                + " status=" + status);
    }
}
