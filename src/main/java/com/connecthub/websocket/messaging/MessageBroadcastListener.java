package com.connecthub.websocket.messaging;

import com.connecthub.websocket.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Consumes message domain events from RabbitMQ queues and broadcasts them
 * to the appropriate STOMP topics so connected WebSocket clients are updated
 * in real-time.
 *
 * This decouples the message-service from the websocket-service:
 * message-service just fires events; this listener does the broadcasting.
 *
 * STOMP destinations used:
 *   /topic/room/{roomId}   → all messages, edits, deletes, reactions, status
 *   /topic/presence        → online/offline status changes
 */
@Component
public class MessageBroadcastListener {

    private static final Logger log = Logger.getLogger(MessageBroadcastListener.class.getName());

    private final SimpMessagingTemplate messagingTemplate;

    public MessageBroadcastListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // ─── New Message ──────────────────────────────────────────────────────────

    /**
     * Consumed from: ws.message.sent
     * Broadcasts the new message to /topic/room/{roomId}
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_WS_MESSAGE_SENT)
    public void handleMessageSent(Map<String, Object> event) {
        Object roomId = event.get("roomId");
        log.info("[RabbitMQ→STOMP] message.sent → /topic/room/" + roomId);
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", event);
        } catch (Exception e) {
            log.warning("[RabbitMQ→STOMP] Failed to broadcast message.sent: " + e.getMessage());
        }
    }

    // ─── Message Edited ───────────────────────────────────────────────────────

    /**
     * Consumed from: ws.message.edited
     * Broadcasts the edit event to /topic/room/{roomId}
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_WS_MESSAGE_EDITED)
    public void handleMessageEdited(Map<String, Object> event) {
        Object roomId = event.get("roomId");
        log.info("[RabbitMQ→STOMP] message.edited → /topic/room/" + roomId);
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", event);
        } catch (Exception e) {
            log.warning("[RabbitMQ→STOMP] Failed to broadcast message.edited: " + e.getMessage());
        }
    }

    // ─── Message Deleted ──────────────────────────────────────────────────────

    /**
     * Consumed from: ws.message.deleted
     * Broadcasts the delete event to /topic/room/{roomId}
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_WS_MESSAGE_DELETED)
    public void handleMessageDeleted(Map<String, Object> event) {
        Object roomId = event.get("roomId");
        log.info("[RabbitMQ→STOMP] message.deleted → /topic/room/" + roomId);
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", event);
        } catch (Exception e) {
            log.warning("[RabbitMQ→STOMP] Failed to broadcast message.deleted: " + e.getMessage());
        }
    }

    // ─── Delivery Status Updated ──────────────────────────────────────────────

    /**
     * Consumed from: ws.message.status
     * Broadcasts delivery status changes (SENT → DELIVERED → READ) to room
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_WS_MESSAGE_STATUS)
    public void handleDeliveryStatus(Map<String, Object> event) {
        Object roomId = event.get("roomId");
        log.info("[RabbitMQ→STOMP] message.status → /topic/room/" + roomId
                + " status=" + event.get("status"));
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", event);
        } catch (Exception e) {
            log.warning("[RabbitMQ→STOMP] Failed to broadcast message.status: " + e.getMessage());
        }
    }

    // ─── Presence Update ──────────────────────────────────────────────────────

    /**
     * Consumed from: ws.presence.update
     * Broadcasts online/offline status changes to /topic/presence
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_WS_PRESENCE_UPDATE)
    public void handlePresenceUpdate(Map<String, Object> event) {
        log.info("[RabbitMQ→STOMP] presence.* → /topic/presence userId=" + event.get("userId")
                + " status=" + event.get("status"));
        try {
            messagingTemplate.convertAndSend("/topic/presence", event);
        } catch (Exception e) {
            log.warning("[RabbitMQ→STOMP] Failed to broadcast presence update: " + e.getMessage());
        }
    }
}
