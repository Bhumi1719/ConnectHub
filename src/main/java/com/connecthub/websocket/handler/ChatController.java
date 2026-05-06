package com.connecthub.websocket.handler;

import com.connecthub.websocket.payload.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Controller
public class ChatController {

    private static final Logger log = Logger.getLogger(ChatController.class.getName());

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${message.service.url}")
    private String messageServiceUrl;

    @Value("${presence.service.url}")
    private String presenceServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Value("${room.service.url}")
    private String roomServiceUrl;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          RestTemplate restTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

    // ─── /app/chat.send ───────────────────────────────────────────────────────
    // Client sends message → save to DB → broadcast to room
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        log.info("Message received from userId=" + chatMessage.getSenderId()
                + " roomId=" + chatMessage.getRoomId()
                + " type=" + chatMessage.getType());

        try {
            validateOutgoingMessage(chatMessage);

            // 1. Save message to Message Service
            Map<String, Object> messageRequest = new HashMap<>();
            messageRequest.put("roomId", chatMessage.getRoomId());
            messageRequest.put("senderId", chatMessage.getSenderId());
            messageRequest.put("content", chatMessage.getContent());
            messageRequest.put("type", chatMessage.getType());
            messageRequest.put("mediaUrl", chatMessage.getMediaUrl());
            messageRequest.put("replyToMessageId", chatMessage.getReplyToMessageId());

            Map savedMessage = restTemplate.postForObject(
                    messageServiceUrl + "/messages",
                    messageRequest,
                    Map.class
            );

            if (savedMessage == null || savedMessage.get("messageId") == null) {
                throw new RuntimeException("Message service returned empty/invalid response");
            }

            // 2. Broadcast saved message to all room subscribers
            // Topic: /topic/room/{roomId}
            messagingTemplate.convertAndSend(
                    "/topic/room/" + chatMessage.getRoomId(),
                    savedMessage
            );

            log.info("Message broadcast to /topic/room/" + chatMessage.getRoomId());

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.severe("Failed to save message. status=" + e.getStatusCode()
                    + " response=" + responseBody);
            notifySenderMessageFailed(
                    chatMessage,
                    "Message service rejected the request ("
                            + e.getStatusCode().value()
                            + ")."
            );
        } catch (Exception e) {
            log.severe("Failed to process message: " + e);
            notifySenderMessageFailed(chatMessage, "Unable to save message. Please retry.");
        }
    }

    // ─── /app/chat.typing ─────────────────────────────────────────────────────
    // Client types → broadcast typing indicator to room
    // NOT saved to DB — transient event only
    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload TypingIndicator typingIndicator) {
        log.info("Typing indicator from userId=" + typingIndicator.getSenderId()
                + " roomId=" + typingIndicator.getRoomId()
                + " isTyping=" + typingIndicator.getIsTyping());

        // Broadcast typing event to room
        // All subscribers see who is typing
        messagingTemplate.convertAndSend(
                "/topic/room/" + typingIndicator.getRoomId(),
                Map.of(
                    "eventType", "TYPING_INDICATOR",
                    "senderId", typingIndicator.getSenderId(),
                    "roomId", typingIndicator.getRoomId(),
                    "isTyping", typingIndicator.getIsTyping()
                )
        );
    }

    // ─── /app/chat.read ───────────────────────────────────────────────────────
    // Client reads messages → update lastReadAt → broadcast read receipt
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

            // 2. Update message delivery status to READ in Message Service
            if (readReceipt.getUpToMessageId() != null) {
                restTemplate.put(
                        messageServiceUrl + "/messages/"
                        + readReceipt.getUpToMessageId() + "/status",
                        Map.of("status", "READ")
                );
            }

            // 3. Broadcast read receipt to room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + readReceipt.getRoomId(),
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
    // Client reacts to message → broadcast reaction to room
    @MessageMapping("/chat.reaction")
    public void reaction(@Payload Reaction reaction) {
        log.info("Reaction from userId=" + reaction.getSenderId()
                + " messageId=" + reaction.getMessageId()
                + " emoji=" + reaction.getEmoji());

        // Broadcast reaction to room
        messagingTemplate.convertAndSend(
                "/topic/room/" + reaction.getRoomId(),
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
    // Client edits message → update in DB → broadcast edit event
    @MessageMapping("/chat.edit")
    public void editMessage(@Payload MessageEdit messageEdit) {
        log.info("Edit message from userId=" + messageEdit.getEditorId()
                + " messageId=" + messageEdit.getMessageId());

        try {
            // 1. Update in Message Service
            restTemplate.put(
                    messageServiceUrl + "/messages/" + messageEdit.getMessageId(),
                    Map.of("content", messageEdit.getNewContent())
            );

            // 2. Broadcast edit event to room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + messageEdit.getRoomId(),
                    Map.of(
                        "eventType", "MESSAGE_EDIT",
                        "editorId", messageEdit.getEditorId(),
                        "messageId", messageEdit.getMessageId(),
                        "roomId", messageEdit.getRoomId(),
                        "newContent", messageEdit.getNewContent()
                    )
            );

        } catch (Exception e) {
            log.warning("Failed to process message edit: " + e.getMessage());
        }
    }

    // ─── /app/chat.delete ─────────────────────────────────────────────────────
    // Client deletes message → soft delete in DB → broadcast delete event
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload MessageDelete messageDelete) {
        log.info("Delete message from userId=" + messageDelete.getDeleterId()
                + " messageId=" + messageDelete.getMessageId());

        try {
            // 1. Soft delete in Message Service
            restTemplate.delete(
                    messageServiceUrl + "/messages/" + messageDelete.getMessageId()
            );

            // 2. Broadcast delete event to room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + messageDelete.getRoomId(),
                    Map.of(
                        "eventType", "MESSAGE_DELETE",
                        "deleterId", messageDelete.getDeleterId(),
                        "messageId", messageDelete.getMessageId(),
                        "roomId", messageDelete.getRoomId()
                    )
            );

        } catch (Exception e) {
            log.warning("Failed to process message delete: " + e.getMessage());
        }
    }

    // ─── /app/presence.update ─────────────────────────────────────────────────
    // Client changes status → update in Presence Service → broadcast
    @MessageMapping("/presence.update")
    public void presenceUpdate(@Payload PresenceUpdate presenceUpdate) {
        log.info("Presence update from userId=" + presenceUpdate.getUserId()
                + " status=" + presenceUpdate.getStatus());

        try {
            // 1. Update in Presence Service
            restTemplate.put(
                    presenceServiceUrl + "/presence/status/" + presenceUpdate.getUserId(),
                    Map.of("status", presenceUpdate.getStatus())
            );

            // 2. Broadcast to all presence subscribers
            messagingTemplate.convertAndSend(
                    "/topic/presence",
                    Map.of(
                        "eventType", "PRESENCE_UPDATE",
                        "userId", presenceUpdate.getUserId(),
                        "status", presenceUpdate.getStatus()
                    )
            );

        } catch (Exception e) {
            log.warning("Failed to process presence update: " + e.getMessage());
        }
    }

    private void validateOutgoingMessage(ChatMessage chatMessage) {
        if (chatMessage.getSenderId() == null || chatMessage.getRoomId() == null) {
            throw new RuntimeException("senderId and roomId are required");
        }

        String type = chatMessage.getType() == null ? "TEXT" : chatMessage.getType().trim();
        chatMessage.setType(type);

        if ("TEXT".equalsIgnoreCase(type)
                && (chatMessage.getContent() == null || chatMessage.getContent().isBlank())) {
            throw new RuntimeException("Content is required for TEXT messages");
        }

        if (("IMAGE".equalsIgnoreCase(type) || "FILE".equalsIgnoreCase(type))
                && (chatMessage.getMediaUrl() == null || chatMessage.getMediaUrl().isBlank())) {
            throw new RuntimeException("mediaUrl is required for IMAGE/FILE messages");
        }
    }

    private void notifySenderMessageFailed(ChatMessage chatMessage, String reason) {
        if (chatMessage == null || chatMessage.getSenderId() == null) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/user/" + chatMessage.getSenderId(),
                Map.of(
                        "eventType", "MESSAGE_SEND_FAILED",
                        "roomId", chatMessage.getRoomId(),
                        "senderId", chatMessage.getSenderId(),
                        "content", chatMessage.getContent() == null ? "" : chatMessage.getContent(),
                        "type", chatMessage.getType() == null ? "TEXT" : chatMessage.getType(),
                        "reason", reason
                )
        );
    }
}
