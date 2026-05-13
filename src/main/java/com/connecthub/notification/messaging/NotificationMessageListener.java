package com.connecthub.notification.messaging;

import com.connecthub.notification.config.RabbitMQConfig;
import com.connecthub.notification.dto.SendNotificationRequest;
import com.connecthub.notification.entity.Notification;
import com.connecthub.notification.repository.NotificationRepository;
import com.connecthub.notification.service.NotifService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Listens for message events from message-service via RabbitMQ.
 *
 * When a new message is sent (message.sent routing key), this listener:
 *   1. Persists a NEW_MESSAGE notification for the room members
 *   2. Optionally sends an email if the recipient is offline
 *
 * This replaces the previous approach where the websocket-service
 * called notification-service via REST on every message.
 */
@Component
public class NotificationMessageListener {

    private static final Logger log = Logger.getLogger(NotificationMessageListener.class.getName());

    private final NotifService notifService;
    private final NotificationRepository notificationRepository;

    public NotificationMessageListener(NotifService notifService,
                                        NotificationRepository notificationRepository) {
        this.notifService = notifService;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Consumes from queue: notification.new-message
     * Triggered when message-service publishes a new chat message.
     *
     * Payload fields (from MessageEventPublisher):
     *   eventType, messageId, roomId, senderId, content, type, mediaUrl,
     *   replyToMessageId, deliveryStatus, sentAt
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_NEW_MESSAGE)
    public void handleNewMessage(Map<String, Object> event) {
        log.info("[RabbitMQ] Received message.sent event: messageId="
                + event.get("messageId") + " roomId=" + event.get("roomId"));

        try {
            Integer roomId   = toInt(event.get("roomId"));
            Integer senderId = toInt(event.get("senderId"));
            Integer messageId = toInt(event.get("messageId"));
            String content   = (String) event.get("content");
            String type      = (String) event.get("type");

            // Build a short preview for the notification title
            String preview = buildPreview(type, content);

            // Create a notification for the room
            // In a real scenario, you'd look up room members and create one per member
            // excluding the sender. Here we create one generic room notification.
            SendNotificationRequest request = new SendNotificationRequest();
            request.setActorId(senderId);
            // recipientId = 0 means "broadcast to room" — adjust to your actual
            // room-member lookup strategy (e.g. call room-service REST endpoint)
            request.setRecipientId(0);
            request.setType("NEW_MESSAGE");
            request.setTitle("New message in room #" + roomId);
            request.setMessage(preview);
            request.setRoomId(roomId);
            request.setMessageId(messageId);

            notifService.send(request);

            log.info("[RabbitMQ] Notification created for roomId=" + roomId
                    + " messageId=" + messageId);

        } catch (Exception e) {
            // Log and swallow — a failed notification must never block the chat flow
            log.warning("[RabbitMQ] Failed to process new-message notification: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildPreview(String type, String content) {
        if ("IMAGE".equals(type)) return "📷 Sent a photo";
        if ("FILE".equals(type))  return "📎 Sent a file";
        if (content == null || content.isBlank()) return "New message";
        return content.length() > 80 ? content.substring(0, 77) + "..." : content;
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.valueOf(value.toString());
    }
}
