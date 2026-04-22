package com.connecthub.notification.resource;

import com.connecthub.notification.dto.BulkNotificationRequest;
import com.connecthub.notification.dto.EmailRequest;
import com.connecthub.notification.dto.SendNotificationRequest;
import com.connecthub.notification.entity.Notification;
import com.connecthub.notification.service.NotifService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotifResource {

    private final NotifService notifService;

    public NotifResource(NotifService notifService) {
        this.notifService = notifService;
    }

    // ─── POST /notifications ──────────────────────────────────────────────────
    // Send single notification
    @PostMapping
    public ResponseEntity<Notification> send(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notifService.send(request));
    }

    // ─── POST /notifications/bulk ─────────────────────────────────────────────
    // Send to multiple users
    @PostMapping("/bulk")
    public ResponseEntity<List<Notification>> sendBulk(
            @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.ok(notifService.sendBulk(request));
    }

    // ─── GET /notifications/user/{recipientId} ────────────────────────────────
    // Get all notifications for a user
    @GetMapping("/user/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(notifService.getByRecipient(recipientId));
    }

    // ─── GET /notifications/user/{recipientId}/unread ─────────────────────────
    // Get only unread notifications
    @GetMapping("/user/{recipientId}/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(notifService.getUnreadByRecipient(recipientId));
    }

    // ─── GET /notifications/user/{recipientId}/count ──────────────────────────
    // Get unread count (shown as badge on UI)
    @GetMapping("/user/{recipientId}/count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @PathVariable Integer recipientId) {
        int count = notifService.getUnreadCount(recipientId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ─── PUT /notifications/{notificationId}/read ─────────────────────────────
    // Mark single notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Integer notificationId) {
        notifService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // ─── PUT /notifications/user/{recipientId}/readall ────────────────────────
    // Mark all notifications as read
    @PutMapping("/user/{recipientId}/readall")
    public ResponseEntity<Map<String, String>> markAllRead(
            @PathVariable Integer recipientId) {
        notifService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ─── DELETE /notifications/{notificationId} ───────────────────────────────
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Integer notificationId) {
        notifService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // ─── POST /notifications/email ────────────────────────────────────────────
    // Send email notification (for missed DMs)
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(
            @RequestBody EmailRequest request) {
        notifService.sendEmail(request);
        return ResponseEntity.ok(Map.of("message", "Email sent"));
    }

    // ─── GET /notifications/all ───────────────────────────────────────────────
    // Admin — get all notifications
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notifService.getAll());
    }
}
