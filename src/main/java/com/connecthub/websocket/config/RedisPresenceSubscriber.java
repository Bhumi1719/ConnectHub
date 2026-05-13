package com.connecthub.websocket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Redis subscriber for the presence channel (ws:presence).
 *
 * When any websocket-service instance publishes a presence update to Redis,
 * ALL instances receive it here and forward it to their local STOMP clients.
 * This makes presence updates work correctly across multiple service instances.
 */
@Component
public class RedisPresenceSubscriber implements MessageListener {

    private static final Logger log = Logger.getLogger(RedisPresenceSubscriber.class.getName());

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisPresenceSubscriber(SimpMessagingTemplate messagingTemplate,
                                   ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper      = objectMapper;
    }

    /**
     * Called by Redis when a message arrives on the ws:presence channel.
     * Forward it to all STOMP clients subscribed to /topic/presence.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            Map<?, ?> payload = objectMapper.readValue(body, Map.class);

            // Broadcast to all STOMP subscribers on this instance
            messagingTemplate.convertAndSend("/topic/presence", payload);

            log.fine("Redis→STOMP presence update: " + payload);
        } catch (Exception e) {
            log.warning("Failed to process Redis presence message: " + e.getMessage());
        }
    }
}