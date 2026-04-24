package com.connecthub.websocket.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class WebSocketEventListener {

    private static final Logger log = Logger.getLogger(WebSocketEventListener.class.getName());

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${presence.service.url}")
    private String presenceServiceUrl;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate,
                                   RestTemplate restTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

    // ─── On WebSocket Connect ─────────────────────────────────────────────────

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Get userId from STOMP connect header
        String userIdStr = accessor.getFirstNativeHeader("userId");

        log.info("WebSocket connected: sessionId=" + sessionId + " userId=" + userIdStr);

        if (userIdStr != null) {
            try {
                Integer userId = Integer.parseInt(userIdStr);

                // 1. Set user online in Presence Service
                Map<String, Object> onlineRequest = new HashMap<>();
                onlineRequest.put("userId", userId);
                onlineRequest.put("sessionId", sessionId);
                onlineRequest.put("deviceType", "WEB");

                restTemplate.postForObject(
                        presenceServiceUrl + "/presence/online",
                        onlineRequest,
                        Map.class
                );

                // 2. Broadcast presence update to all
                messagingTemplate.convertAndSend(
                        "/topic/presence",
                        Map.of(
                            "eventType", "PRESENCE_UPDATE",
                            "userId", userId,
                            "status", "ONLINE"
                        )
                );

                log.info("User set ONLINE: userId=" + userId);

            } catch (Exception e) {
                log.warning("Failed to set user online: " + e.getMessage());
            }
        }
    }

    // ─── On WebSocket Disconnect ──────────────────────────────────────────────

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Get userId from session
        String userIdStr = accessor.getFirstNativeHeader("userId");

        log.info("WebSocket disconnected: sessionId=" + sessionId + " userId=" + userIdStr);

        if (userIdStr != null) {
            try {
                Integer userId = Integer.parseInt(userIdStr);

                // 1. Set user offline in Presence Service
                restTemplate.put(
                        presenceServiceUrl + "/presence/offline/" + userId,
                        null
                );

                // 2. Broadcast offline status to all
                messagingTemplate.convertAndSend(
                        "/topic/presence",
                        Map.of(
                            "eventType", "PRESENCE_UPDATE",
                            "userId", userId,
                            "status", "OFFLINE"
                        )
                );

                log.info("User set OFFLINE: userId=" + userId);

            } catch (Exception e) {
                log.warning("Failed to set user offline: " + e.getMessage());
            }
        }
    }
}
