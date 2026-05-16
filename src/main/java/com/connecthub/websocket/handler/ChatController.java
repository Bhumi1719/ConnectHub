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
 * FIXED FLOW for chat.send:
 *   1. Frontend pehle HTTP POST /messages se message save karta hai.
 *   2. Save hone ke baad frontend WebSocket /app/chat.send pe saved message bhejta hai
 *      (messageId set hota hai).
 *   3. Yahan agar messageId present hai → sirf STOMP broadcast karo (no double save).
 *   4. Agar messageId absent hai (direct WebSocket send without HTTP) → message-service
 *      se save karo, phir broadcast karo.
 *
 * - chat.typing  → direct STOMP broadcast (transient)
 * - chat.read    → REST calls + direct STOMP broadcast
 * - chat.reaction → direct STOMP broadcast (transient)
 * - chat.edit    → message-service REST → RabbitMQ → MessageBroadcastListener
 * - chat.delete  → message-service REST → RabbitMQ → MessageBroadcastListener
 * - presence.update → RabbitMQ presence exchange
 */
@Controller
public class ChatController {

    private static final Logger log = Logger.getLogger(ChatController.class.getName());

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final PresenceEventPublisher presenceEventPublisher;

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
    /**
     * ✅ FIXED:
     * - messageId present  → message already saved by HTTP POST; sirf broadcast karo.
     *   (Double save hona band)
     * - messageId absent   → fallback: message-service se save karo phir broadcast.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        log.info("Message received from userId=" + chatMessage.getSenderId()
                + " roomId=" + chatMessage.getRoomId());

        try {
            // ✅ FIX: messageId present hai matlab HTTP se already save ho chuka hai.
            // Sirf STOMP broadcast karo — double persist NAHI karo.
            if (chatMessage.getMessageId() != null) {
                log.info("Message already persisted (messageId=" + chatMessage.getMessageId()
                        + "), broadcasting only.");
                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + chatMessage.getRoomId() + "/messages",
                        chatMessage
                );
                return;
            }

            // Fallback: agar koi client seedha WebSocket se message bheje bina HTTP ke
            log.info("No messageId — persisting via message-service (fallback path)");
            Map<String, Object> messageRequest = new HashMap<>();
            messageRequest.put("roomId", chatMessage.getRoomId());
            messageRequest.put("senderId", chatMessage.getSenderId());
            messageRequest.put("content", chatMessage.getContent());
            messageRequest.put("type", chatMessage.getType() != null ? chatMessage.getType() : "TEXT");
            messageRequest.put("mediaUrl", chatMessage.getMediaUrl());
            messageRequest.put("replyToMessageId", chatMessage.getReplyToMessageId());

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
                log.info("Fallback: Message saved and broadcast via STOMP");
            }

        } catch (Exception e) {
            log.severe("Failed to process message: " + e.getMessage());
        }
    }

    // ─── /app/chat.typing ─────────────────────────────────────────────────────
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
                    "isTyping", typingIndicator.getIsTyping(),
                    "username", typingIndicator.getUsername() != null ? typingIndicator.getUsername() : ""
                )
        );
    }

    // ─── /app/dm.send ─────────────────────────────────────────────────────────
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

    // ─── /app/dm.typing ───────────────────────────────────────────────────────
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

    // ─── /app/chat.read ───────────────────────────────────────────────────────
    @MessageMapping("/chat.read")
    public void readReceipt(@Payload ReadReceipt readReceipt) {
        log.info("Read receipt from userId=" + readReceipt.getReaderId()
                + " roomId=" + readReceipt.getRoomId()
                + " upToMessageId=" + readReceipt.getUpToMessageId());

        try {
            restTemplate.put(
                    roomServiceUrl + "/rooms/" + readReceipt.getRoomId()
                    + "/lastread/" + readReceipt.getReaderId(),
                    null
            );

            if (readReceipt.getUpToMessageId() != null) {
                restTemplate.put(
                        messageServiceUrl + "/messages/"
                        + readReceipt.getUpToMessageId() + "/status",
                        Map.of("status", "READ")
                );
            }

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
    @MessageMapping("/chat.edit")
    public void editMessage(@Payload MessageEdit messageEdit) {
        log.info("Edit message from userId=" + messageEdit.getEditorId()
                + " messageId=" + messageEdit.getMessageId());

        try {
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
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDelete messageDelete) {
        log.info("Delete message from userId=" + messageDelete.getDeleterId()
                + " messageId=" + messageDelete.getMessageId());

        try {
            restTemplate.delete(
                    messageServiceUrl + "/messages/" + messageDelete.getMessageId()
            );
            log.info("Delete sent to message-service, RabbitMQ event will broadcast it");

        } catch (Exception e) {
            log.warning("Failed to process message delete: " + e.getMessage());
        }
    }

    // ─── /app/presence.update ─────────────────────────────────────────────────
    @MessageMapping("/presence.update")
    public void presenceUpdate(@Payload PresenceUpdate presenceUpdate) {
        log.info("Presence update from userId=" + presenceUpdate.getUserId()
                + " status=" + presenceUpdate.getStatus());

        presenceEventPublisher.publishPresenceUpdate(
                presenceUpdate.getUserId(),
                presenceUpdate.getStatus()
        );
    }
}