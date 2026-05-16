package com.connecthub.message.messaging;

import com.connecthub.message.config.RabbitMQConfig;
import com.connecthub.message.entity.Message;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Publishes message domain events to the RabbitMQ topic exchange.
 *
 * Consumers:
 *   - notification-service listens on ROUTING_MESSAGE_SENT
 *   - websocket-service listens on all routing keys to broadcast via STOMP
 */
@Component
public class MessageEventPublisher {

    private static final Logger log = Logger.getLogger(MessageEventPublisher.class.getName());

    private final RabbitTemplate rabbitTemplate;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish event when a new message is saved.
     * Routing key: message.sent
     */
    public void publishMessageSent(Message message) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MESSAGE_SENT");
        event.put("messageId", message.getMessageId());
        event.put("roomId", message.getRoomId());
        event.put("senderId", message.getSenderId());
        event.put("content", message.getContent());
        event.put("type", message.getType());
        event.put("mediaUrl", message.getMediaUrl());
        event.put("replyToMessageId", message.getReplyToMessageId());
        event.put("deliveryStatus", message.getDeliveryStatus());
        event.put("sentAt", message.getSentAt() != null ? message.getSentAt().toString() : null);

        publish(RabbitMQConfig.ROUTING_MESSAGE_SENT, event,
                "message.sent -> messageId=" + message.getMessageId() + " roomId=" + message.getRoomId());
    }

    /**
     * Publish event when a message is edited.
     * Routing key: message.edited
     */
    public void publishMessageEdited(Message message) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MESSAGE_EDITED");
        event.put("messageId", message.getMessageId());
        event.put("roomId", message.getRoomId());
        event.put("senderId", message.getSenderId());
        event.put("content", message.getContent());
        event.put("editedAt", message.getEditedAt() != null ? message.getEditedAt().toString() : null);

        publish(RabbitMQConfig.ROUTING_MESSAGE_EDITED, event,
                "message.edited -> messageId=" + message.getMessageId());
    }

    /**
     * Publish event when a message is soft-deleted.
     * Routing key: message.deleted
     */
    public void publishMessageDeleted(Integer messageId, Integer roomId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MESSAGE_DELETED");
        event.put("messageId", messageId);
        event.put("roomId", roomId);

        publish(RabbitMQConfig.ROUTING_MESSAGE_DELETED, event,
                "message.deleted -> messageId=" + messageId);
    }

    /**
     * Publish event when delivery status changes (SENT -> DELIVERED -> READ).
     * Routing key: message.status
     */
    public void publishDeliveryStatusUpdated(Integer messageId, Integer roomId,
                                             Integer senderId, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "DELIVERY_STATUS_UPDATED");
        event.put("messageId", messageId);
        event.put("roomId", roomId);
        event.put("senderId", senderId);
        event.put("status", status);

        publish(RabbitMQConfig.ROUTING_MESSAGE_STATUS, event,
                "message.status -> messageId=" + messageId + " status=" + status);
    }

    private void publish(String routingKey, Map<String, Object> event, String description) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.MESSAGES_EXCHANGE, routingKey, event);
            log.info("[RabbitMQ] Published " + description);
        } catch (AmqpException ex) {
            log.warning("[RabbitMQ] Publish skipped (" + description + "): " + ex.getMessage());
        }
    }
}
