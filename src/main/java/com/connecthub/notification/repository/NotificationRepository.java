package com.connecthub.notification.repository;

import com.connecthub.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // All notifications for a user newest first
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId);

    // Unread notifications for a user
    List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(
            Integer recipientId, Boolean isRead);

    // Count unread notifications
    int countByRecipientIdAndIsRead(Integer recipientId, Boolean isRead);

    // Notifications by type
    List<Notification> findByType(String type);

    // Notifications for a room
    List<Notification> findByRoomId(Integer roomId);

    // Mark all as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsRead(@Param("recipientId") Integer recipientId);

    void deleteByNotificationId(Integer notificationId);

    // Delete all read notifications for a user
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = true")
    void deleteReadByRecipientId(@Param("recipientId") Integer recipientId);
}
