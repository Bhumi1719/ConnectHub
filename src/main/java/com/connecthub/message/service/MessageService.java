package com.connecthub.message.service;

import com.connecthub.message.dto.EditMessageRequest;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.entity.Message;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageService {

    // Send a new message — persists and returns saved message
    Message sendMessage(SendMessageRequest request);

    // Get single message by ID
    Message getMessageById(Integer messageId);

    // Get paginated messages for a room (newest first — for infinite scroll)
    Page<Message> getMessagesByRoom(Integer roomId, int page, int size);

    // Get messages before a timestamp (load older messages)
    List<Message> getMessagesBefore(Integer roomId, LocalDateTime before);

    // Edit a message content
    Message editMessage(Integer messageId, EditMessageRequest request);

    // Soft delete a message
    void deleteMessage(Integer messageId);

    // Search messages in a room by keyword
    List<Message> searchMessages(Integer roomId, String keyword);

    // Update delivery status: SENT → DELIVERED → READ
    void updateDeliveryStatus(Integer messageId, String status);

    // Get total message count in a room
    long getMessageCount(Integer roomId);

    // Get unread messages after lastReadAt
    List<Message> getUnreadMessages(Integer roomId, LocalDateTime since);

    // Get unread message count after lastReadAt, excluding the current user's own messages
    long getUnreadCount(Integer roomId, Integer userId, LocalDateTime since);
}
