package com.connecthub.websocket.handler;

import com.connecthub.websocket.messaging.PresenceEventPublisher;
import com.connecthub.websocket.payload.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles inbound STOMP messages from connected clients.
 *
 * With RabbitMQ integration:
 *   - chat.send    → calls message-service REST to persist, then message-service
 *                    publishes the event to RabbitMQ; MessageBroadcastListener
 *                    in THIS service picks it up and broadcasts via STOMP.
 *   - chat.typing  → still direct STOMP broadcast (transient, no persistence needed)
 *   - chat.read    → calls room+message REST, then broadcasts via STOMP
 *   - chat.reaction → still direct STOMP broadcast (transient)
 *   - chat.edit    → calls message-service REST; message-service publishes to RabbitMQ
 *   - chat.delete  → calls message-service REST; message-service publishes to RabbitMQ
 *   - presence.update → publishes to RabbitMQ presence exchange;
 *                        presence-service updates DB, ws-service broadcasts
 */
@Controller
public class ChatController {

    private static final Logger log = Logger.getLogger(ChatController.class.getName());

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final PresenceEventPublisher presenceEventPublisher; // ✅ RabbitMQ publisher

    @Value("${message.service.url}")
    private String messageServiceUrl;

    @Value("${presence.service.url}")
    private String presenceServiceUrl;

    @Value("${room.service.url}")
    private String roomServiceUrl;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          RestTemplate restTemplate,
                          PresenceEventPublisher presenceEventPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
        this.presenceEventPublisher = presenceEventPublisher;
    }

    // ─── /app/chat.send ───────────────────────────────────────────────────────
    // Client sends message → persist via REST → message-service publishes to RabbitMQ
    // → MessageBroadcastListener.handleMessageSent() broadcasts to /topic/room/{roomId}
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        log.info("Message received from userId=" + chatMessage.getSenderId()
                + " roomId=" + chatMessage.getRoomId());

        try {
            if (chatMessage.getMessageId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + chatMessage.getRoomId() + "/messages",
                        chatMessage
                );
                return;
            }

            Map<String, Object> messageRequest = new HashMap<>();
            messageRequest.put("roomId", chatMessage.getRoomId());
            messageRequest.put("senderId", chatMessage.getSenderId());
            messageRequest.put("content", chatMessage.getContent());
            messageRequest.put("type", chatMessage.getType());
            messageRequest.put("mediaUrl", chatMessage.getMediaUrl());
            messageRequest.put("replyToMessageId", chatMessage.getReplyToMessageId());

            // Persist message — message-service will publish to RabbitMQ automatically
            // No need to call messagingTemplate here — MessageBroadcastListener handles it
            Map savedMessage = restTemplate.postForObject(
                    messageServiceUrl + "/messages",
                    messageRequest,
                    Map.class
            );

            if (savedMessage != null) {
                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + chatMessage.getRoomId() + "/messages",
                        savedMessage
                );
            }

            log.info("Message sent to message-service, RabbitMQ event will broadcast it");

        } catch (Exception e) {
            log.severe("Failed to process message: " + e.getMessage());
        }
    }

    // ─── /app/chat.typing ─────────────────────────────────────────────────────
    // Transient event — no persistence, no RabbitMQ needed, direct STOMP is fine
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingIndicator typingIndicator) {
        log.info("Typing indicator from userId=" + typingIndicator.getSenderId()
                + " roomId=" + typingIndicator.getRoomId()
                + " isTyping=" + typingIndicator.getIsTyping());

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + typingIndicator.getRoomId() + "/typing",
                Map.of(
                    "eventType", "TYPING_INDICATOR",
                    "senderId", typingIndicator.getSenderId(),
                    "roomId", typingIndicator.getRoomId(),
                    "isTyping", typingIndicator.getIsTyping()
                )
        );
    }

    // ─── /app/chat.read ───────────────────────────────────────────────────────
    // Updates room + message status via REST, then broadcasts via STOMP directly.
    // The status update will ALSO trigger a RabbitMQ event from message-service
    // (ws.message.status queue), but for read receipts it's acceptable to also
    // do a direct STOMP broadcast for lower latency.
    @MessageMapping("/dm.send")
    public void sendDirectMessage(@Payload ChatMessage chatMessage) {
        if (chatMessage.getRecipientId() == null) {
            log.warning("DM ignored because recipientId is missing");
            return;
        }

        log.info("DM received from userId=" + chatMessage.getSenderId()
                + " recipientId=" + chatMessage.getRecipientId());

        messagingTemplate.convertAndSend(
                "/topic/users/" + chatMessage.getRecipientId() + "/dm",
                chatMessage
        );
    }

    @MessageMapping("/dm.typing")
    public void directTyping(@Payload Map<String, Object> typingEvent) {
        Object recipientId = typingEvent.get("to");
        if (recipientId == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/users/" + recipientId + "/typing",
                typingEvent
        );
    }

    @MessageMapping("/chat.read")
    public void readReceipt(@Payload ReadReceipt readReceipt) {
        log.info("Read receipt from userId=" + readReceipt.getReaderId()
                + " roomId=" + readReceipt.getRoomId()
                + " upToMessageId=" + readReceipt.getUpToMessageId());

        try {
            // 1. Update lastReadAt in Room Service
            restTemplate.put(
                    roomServiceUrl + "/rooms/" + readReceipt.getRoomId()
                    + "/lastread/" + readReceipt.getReaderId(),
                    null
            );

            // 2. Update message delivery status (triggers RabbitMQ publish in message-service)
            if (readReceipt.getUpToMessageId() != null) {
                restTemplate.put(
                        messageServiceUrl + "/messages/"
                        + readReceipt.getUpToMessageId() + "/status",
                        Map.of("status", "READ")
                );
            }

            // 3. Direct STOMP broadcast for immediate feedback
            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + readReceipt.getRoomId() + "/messages",
                    Map.of(
                        "eventType", "READ_RECEIPT",
                        "readerId", readReceipt.getReaderId(),
                        "roomId", readReceipt.getRoomId(),
                        "upToMessageId", readReceipt.getUpToMessageId()
                    )
            );

        } catch (Exception e) {
            log.warning("Failed to process read receipt: " + e.getMessage());
        }
    }

    // ─── /app/chat.reaction ───────────────────────────────────────────────────
    // Transient — direct STOMP broadcast, no RabbitMQ needed
    @MessageMapping("/chat.reaction")
    public void reaction(@Payload Reaction reaction) {
        log.info("Reaction from userId=" + reaction.getSenderId()
                + " messageId=" + reaction.getMessageId()
                + " emoji=" + reaction.getEmoji());

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + reaction.getRoomId() + "/messages",
                Map.of(
                    "eventType", "REACTION",
                    "senderId", reaction.getSenderId(),
                    "messageId", reaction.getMessageId(),
                    "roomId", reaction.getRoomId(),
                    "emoji", reaction.getEmoji()
                )
        );
    }

    // ─── /app/chat.edit ───────────────────────────────────────────────────────
    // Persists edit via REST → message-service publishes MESSAGE_EDITED to RabbitMQ
    // → MessageBroadcastListener.handleMessageEdited() broadcasts to STOMP
    @MessageMapping("/chat.edit")
    public void editMessage(@Payload MessageEdit messageEdit) {
        log.info("Edit message from userId=" + messageEdit.getEditorId()
                + " messageId=" + messageEdit.getMessageId());

        try {
            // REST call to message-service — it will publish to RabbitMQ
            restTemplate.put(
                    messageServiceUrl + "/messages/" + messageEdit.getMessageId(),
                    Map.of("content", messageEdit.getNewContent())
            );
            log.info("Edit sent to message-service, RabbitMQ event will broadcast it");

        } catch (Exception e) {
            log.warning("Failed to process message edit: " + e.getMessage());
        }
    }

    // ─── /app/chat.delete ─────────────────────────────────────────────────────
    // Persists delete via REST → message-service publishes MESSAGE_DELETED to RabbitMQ
    // → MessageBroadcastListener.handleMessageDeleted() broadcasts to STOMP
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDelete messageDelete) {
        log.info("Delete message from userId=" + messageDelete.getDeleterId()
                + " messageId=" + messageDelete.getMessageId());

        try {
            // REST call to message-service — it will publish to RabbitMQ
            restTemplate.delete(
                    messageServiceUrl + "/messages/" + messageDelete.getMessageId()
            );
            log.info("Delete sent to message-service, RabbitMQ event will broadcast it");

        } catch (Exception e) {
            log.warning("Failed to process message delete: " + e.getMessage());
        }
    }

    // ─── /app/presence.update ─────────────────────────────────────────────────
    // ✅ Now publishes to RabbitMQ presence exchange instead of calling REST directly.
    // Presence-service consumes, updates DB, and also the ws.presence.update queue
    // triggers MessageBroadcastListener.handlePresenceUpdate() → STOMP broadcast.
    @MessageMapping("/presence.update")
    public void presenceUpdate(@Payload PresenceUpdate presenceUpdate) {
        log.info("Presence update from userId=" + presenceUpdate.getUserId()
                + " status=" + presenceUpdate.getStatus());

        // ✅ Publish to RabbitMQ — presence-service and websocket-service both react
        presenceEventPublisher.publishPresenceUpdate(
                presenceUpdate.getUserId(),
                presenceUpdate.getStatus()
        );
    }
}
