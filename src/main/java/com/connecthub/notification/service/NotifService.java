package com.connecthub.notification.service;

import com.connecthub.notification.dto.BulkNotificationRequest;
import com.connecthub.notification.dto.EmailRequest;
import com.connecthub.notification.dto.SendNotificationRequest;
import com.connecthub.notification.entity.Notification;

import java.util.List;

public interface NotifService {

    // Send single notification
    Notification send(SendNotificationRequest request);

    // Send to multiple users at once (e.g. room invite to all members)
    List<Notification> sendBulk(BulkNotificationRequest request);

    // Get all notifications for a user
    List<Notification> getByRecipient(Integer recipientId);

    // Get only unread notifications
    List<Notification> getUnreadByRecipient(Integer recipientId);

    // Get unread count
    int getUnreadCount(Integer recipientId);

    // Mark single notification as read
    void markAsRead(Integer notificationId);

    // Mark all notifications as read for a user
    void markAllRead(Integer recipientId);

    // Delete a notification
    void deleteNotification(Integer notificationId);

    // Send email notification (for missed DMs)
    void sendEmail(EmailRequest request);

    // Get all notifications (admin)
    List<Notification> getAll();
}
