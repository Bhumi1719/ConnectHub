package com.connecthub.message.repository;

import com.connecthub.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    // Get messages for a room — newest first, paginated (infinite scroll)
    Page<Message> findByRoomIdOrderBySentAtDesc(Integer roomId, Pageable pageable);

    // Get messages before a certain time (for loading older messages)
    List<Message> findByRoomIdAndSentAtBeforeOrderBySentAtDesc(Integer roomId, LocalDateTime before);

    Optional<Message> findByMessageId(Integer messageId);

    List<Message> findBySenderId(Integer senderId);

    // Full-text search within a room
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.sentAt DESC")
    List<Message> searchInRoom(@Param("roomId") Integer roomId, @Param("keyword") String keyword);

    // Count total messages in a room
    long countByRoomId(Integer roomId);

    // Get unread messages after lastReadAt
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.sentAt > :since AND m.isDeleted = false ORDER BY m.sentAt ASC")
    List<Message> findUnreadMessages(@Param("roomId") Integer roomId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.senderId <> :userId AND m.sentAt > :since AND m.isDeleted = false")
    long countUnreadMessages(@Param("roomId") Integer roomId,
                             @Param("userId") Integer userId,
                             @Param("since") LocalDateTime since);

    void deleteByMessageId(Integer messageId);
}
