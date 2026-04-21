package com.connecthub.message.service.impl;

import com.connecthub.message.dto.EditMessageRequest;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.service.MessageService;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${room.service.url}")
    private String roomServiceUrl;

    public MessageServiceImpl(MessageRepository messageRepository,
                              RestTemplate restTemplate) {
        this.messageRepository = messageRepository;
        this.restTemplate = restTemplate;
    }

    // ─── Send Message ─────────────────────────────────────────────────────────

    @Override
    public Message sendMessage(SendMessageRequest request) {
        // Validate content for TEXT messages
        if ("TEXT".equals(request.getType()) &&
            (request.getContent() == null || request.getContent().isBlank())) {
            throw new RuntimeException("Content is required for TEXT messages");
        }

        // Validate mediaUrl for IMAGE/FILE messages
        if (("IMAGE".equals(request.getType()) || "FILE".equals(request.getType())) &&
            (request.getMediaUrl() == null || request.getMediaUrl().isBlank())) {
            throw new RuntimeException("mediaUrl is required for IMAGE/FILE messages");
        }

        Message message = Message.builder()
                .roomId(request.getRoomId())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : "TEXT")
                .mediaUrl(request.getMediaUrl())
                .replyToMessageId(request.getReplyToMessageId())
                .deliveryStatus("SENT")
                .build();

        Message saved = messageRepository.save(message);
        log.info("Message saved: id=" + saved.getMessageId() + " roomId=" + saved.getRoomId());

        // Notify Room Service to update lastMessageAt
        updateRoomLastMessage(request.getRoomId());

        return saved;
    }

    // ─── Get Message By ID ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Message getMessageById(Integer messageId) {
        return messageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
    }

    // ─── Get Messages By Room (Paginated) ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
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
        return updated;
    }

    // ─── Delete Message (Soft Delete) ─────────────────────────────────────────

    @Override
    public void deleteMessage(Integer messageId) {
        Message message = getMessageById(messageId);
        message.setIsDeleted(true);
        message.setContent("[This message was deleted]");
        messageRepository.save(message);
        log.info("Message soft-deleted: id=" + messageId);
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
