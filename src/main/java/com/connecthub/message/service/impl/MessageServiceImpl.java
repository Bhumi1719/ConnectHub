package com.connecthub.message.service.impl;

import com.connecthub.message.config.RabbitMQConfig;
import com.connecthub.message.dto.EditMessageRequest;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.messaging.MessageEventPublisher;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.service.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private static final Logger log = Logger.getLogger(MessageServiceImpl.class.getName());

    private final MessageRepository messageRepository;
    private final RestTemplate restTemplate;
    private final MessageEventPublisher eventPublisher;   // ✅ RabbitMQ publisher

    @Value("${room.service.url}")
    private String roomServiceUrl;

    public MessageServiceImpl(MessageRepository messageRepository,
                              RestTemplate restTemplate,
                              MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.restTemplate = restTemplate;
        this.eventPublisher = eventPublisher;
    }

    // ─── Send Message ─────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "messages", key = "#request.roomId")   // ✅ evict room cache on new message
    public Message sendMessage(SendMessageRequest request) {
        String type = request.getType() == null || request.getType().isBlank()
                ? "TEXT"
                : request.getType().trim().toUpperCase();

        // Validate content for TEXT messages
        if ("TEXT".equals(type) &&
            (request.getContent() == null || request.getContent().isBlank())) {
            throw new RuntimeException("Content is required for TEXT messages");
        }

        // Validate mediaUrl for IMAGE/FILE messages
        if (("IMAGE".equals(type) || "FILE".equals(type)) &&
            (request.getMediaUrl() == null || request.getMediaUrl().isBlank())) {
            throw new RuntimeException("mediaUrl is required for IMAGE/FILE messages");
        }

        Message message = Message.builder()
                .roomId(request.getRoomId())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .type(type)
                .mediaUrl(request.getMediaUrl())
                .replyToMessageId(request.getReplyToMessageId())
                .deliveryStatus("SENT")
                .build();

        Message saved = messageRepository.save(message);
        log.info("Message saved: id=" + saved.getMessageId() + " roomId=" + saved.getRoomId());

        // Notify Room Service to update lastMessageAt (still via REST — synchronous, lightweight)
        updateRoomLastMessage(request.getRoomId());

        // ✅ Publish to RabbitMQ — notification-service and websocket-service will react
        eventPublisher.publishMessageSent(saved);

        return saved;
    }

    // ─── Get Message By ID ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "message", key = "#messageId")           // ✅ cache individual message by id
    public Message getMessageById(Integer messageId) {
        return messageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
    }

    // ─── Get Messages By Room (Paginated) ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "messages", key = "#roomId + ':' + #page + ':' + #size") // ✅ cache paginated room messages
    public Page<Message> getMessagesByRoom(Integer roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }

    // ─── Get Messages Before Timestamp ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessagesBefore(Integer roomId, LocalDateTime before) {
        return messageRepository.findByRoomIdAndSentAtBeforeOrderBySentAtDesc(roomId, before);
    }

    // ─── Edit Message ─────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = {"message", "messages"}, allEntries = true) // ✅ evict on edit
    public Message editMessage(Integer messageId, EditMessageRequest request) {
        Message message = getMessageById(messageId);

        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit a deleted message");
        }
        if (!"TEXT".equals(message.getType())) {
            throw new RuntimeException("Only TEXT messages can be edited");
        }

        message.setContent(request.getContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message updated = messageRepository.save(message);
        log.info("Message edited: id=" + messageId);

        // ✅ Publish edit event — websocket-service will broadcast to room subscribers
        eventPublisher.publishMessageEdited(updated);

        return updated;
    }

    // ─── Delete Message (Soft Delete) ─────────────────────────────────────────

    @Override
    @CacheEvict(value = {"message", "messages"}, allEntries = true) // ✅ evict on delete
    public void deleteMessage(Integer messageId) {
        Message message = getMessageById(messageId);
        Integer roomId = message.getRoomId();

        message.setIsDeleted(true);
        message.setContent("[This message was deleted]");
        messageRepository.save(message);
        log.info("Message soft-deleted: id=" + messageId);

        // ✅ Publish delete event — websocket-service will broadcast to room subscribers
        eventPublisher.publishMessageDeleted(messageId, roomId);
    }

    // ─── Search Messages ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Message> searchMessages(Integer roomId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new RuntimeException("Search keyword cannot be empty");
        }
        return messageRepository.searchInRoom(roomId, keyword);
    }

    // ─── Update Delivery Status ───────────────────────────────────────────────

    @Override
    public void updateDeliveryStatus(Integer messageId, String status) {
        List<String> allowed = List.of("SENT", "DELIVERED", "READ");
        if (!allowed.contains(status)) {
            throw new RuntimeException("Invalid status. Allowed: SENT, DELIVERED, READ");
        }
        Message message = getMessageById(messageId);
        message.setDeliveryStatus(status);
        messageRepository.save(message);
        log.info("Delivery status updated to " + status + " for messageId=" + messageId);

        // ✅ Publish status update — websocket-service will broadcast read receipts
        eventPublisher.publishDeliveryStatusUpdated(
                messageId, message.getRoomId(), message.getSenderId(), status);
    }

    // ─── Get Message Count ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long getMessageCount(Integer roomId) {
        return messageRepository.countByRoomId(roomId);
    }

    // ─── Get Unread Messages ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(Integer roomId, LocalDateTime since) {
        return messageRepository.findUnreadMessages(roomId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer roomId, Integer userId, LocalDateTime since) {
        return messageRepository.countUnreadMessages(roomId, userId, since);
    }

    // ─── Helper — Notify Room Service ─────────────────────────────────────────

    private void updateRoomLastMessage(Integer roomId) {
        try {
            String url = roomServiceUrl + "/rooms/" + roomId + "/lastmessage";
            restTemplate.put(url, null);
        } catch (Exception e) {
            // Don't fail message sending if room service is down
            log.warning("Could not update lastMessageAt for roomId=" + roomId + ": " + e.getMessage());
        }
    }
}
