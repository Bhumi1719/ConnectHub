package com.connecthub.websocket.handler;

import com.connecthub.websocket.messaging.PresenceEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Listens for WebSocket session lifecycle events and publishes to RabbitMQ.
 *
 * On connect  → publishes presence.online  → presence-service sets ONLINE in DB
 *            → stores sessionId → userId mapping in Redis (TTL 1 hour)
 * On disconnect → publishes presence.offline → presence-service sets INVISIBLE in DB
 *              → removes sessionId → userId mapping from Redis
 *
 * Redis keys:
 *   ws:session:{sessionId} → userId   (lookup userId from session)
 *   ws:user:{userId}       → sessionId (lookup session from userId)
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = Logger.getLogger(WebSocketEventListener.class.getName());

    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final String USER_KEY_PREFIX    = "ws:user:";
    private static final Duration SESSION_TTL      = Duration.ofHours(1);

    private final PresenceEventPublisher presenceEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;  // ✅ Redis — session registry

    public WebSocketEventListener(PresenceEventPublisher presenceEventPublisher,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.presenceEventPublisher = presenceEventPublisher;
        this.redisTemplate          = redisTemplate;
    }

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Extract userId from session attributes (set during handshake by SecurityConfig)
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null || !attrs.containsKey("userId")) {
            log.warning("WebSocket connected without userId attribute: sessionId=" + sessionId);
            return;
        }

        Integer userId     = (Integer) attrs.get("userId");
        String  deviceType = (String)  attrs.getOrDefault("deviceType", "WEB");
        String  ipAddress  = (String)  attrs.getOrDefault("ipAddress",  null);

        log.info("WebSocket connected: sessionId=" + sessionId + " userId=" + userId);

        // ✅ Store session ↔ userId mapping in Redis
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId, userId,    SESSION_TTL);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX    + userId,    sessionId, SESSION_TTL);

        // ✅ Publish presence.online to RabbitMQ
        presenceEventPublisher.publishUserOnline(userId, sessionId, deviceType, ipAddress);
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null || !attrs.containsKey("userId")) {
            log.warning("WebSocket disconnected without userId attribute: sessionId=" + sessionId);
            return;
        }

        Integer userId = (Integer) attrs.get("userId");

        log.info("WebSocket disconnected: sessionId=" + sessionId + " userId=" + userId);

        // ✅ Remove session ↔ userId mapping from Redis on disconnect
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        redisTemplate.delete(USER_KEY_PREFIX    + userId);

        // ✅ Publish presence.offline to RabbitMQ
        presenceEventPublisher.publishUserOffline(userId);
    }
}
