package com.connecthub.notification.resource;

import com.connecthub.notification.dto.BulkNotificationRequest;
import com.connecthub.notification.dto.EmailRequest;
import com.connecthub.notification.dto.SendNotificationRequest;
import com.connecthub.notification.entity.Notification;
import com.connecthub.notification.service.NotifService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name        = "Notifications",
    description = "Send push/in-app/email notifications. Mark as read. " +
                  "Get unread badge counts. Supports single and bulk dispatch."
)
public class NotifResource {

    private final NotifService notifService;

    public NotifResource(NotifService notifService) {
        this.notifService = notifService;
    }

    // ─── POST /notifications ──────────────────────────────────────────────────

    @Operation(
        summary     = "Send a single notification",
        description = "Sends an in-app notification to a specific user. " +
                      "Supported types: MESSAGE, MENTION, ROOM_INVITE, SYSTEM."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification sent",
            content = @Content(schema = @Schema(implementation = Notification.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT missing or invalid", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Notification> send(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Notification payload",
                required    = true,
                content     = @Content(
                    schema   = @Schema(implementation = SendNotificationRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "recipientId": 2,
                          "senderId": 1,
                          "type": "MESSAGE",
                          "title": "New message from John",
                          "body": "Hey, are you free?",
                          "referenceId": 42
                        }
                        """)
                )
            )
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notifService.send(request));
    }

    // ─── POST /notifications/bulk ─────────────────────────────────────────────

    @Operation(
        summary     = "Send notification to multiple users",
        description = "Bulk dispatch — useful for room-wide mentions or system announcements."
    )
    @ApiResponse(responseCode = "200", description = "Notifications sent to all recipients")
    @PostMapping("/bulk")
    public ResponseEntity<List<Notification>> sendBulk(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Bulk notification payload",
                content     = @Content(
                    schema   = @Schema(implementation = BulkNotificationRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "recipientIds": [2, 3, 4],
                          "senderId": 1,
                          "type": "MENTION",
                          "title": "You were mentioned",
                          "body": "@team please review the PR",
                          "referenceId": 10
                        }
                        """)
                )
            )
            @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.ok(notifService.sendBulk(request));
    }

    // ─── GET /notifications/user/{recipientId} ────────────────────────────────

    @Operation(summary = "Get all notifications for a user")
    @ApiResponse(responseCode = "200", description = "Notification list returned")
    @GetMapping("/user/{recipientId}")
    public ResponseEntity<List<Notification>> getByRecipient(
            @Parameter(description = "Recipient user ID", example = "1")
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(notifService.getByRecipient(recipientId));
    }

    // ─── GET /notifications/user/{recipientId}/unread ─────────────────────────

    @Operation(
        summary     = "Get unread notifications for a user",
        description = "Returns only notifications where `isRead = false`."
    )
    @ApiResponse(responseCode = "200", description = "Unread notifications returned")
    @GetMapping("/user/{recipientId}/unread")
    public ResponseEntity<List<Notification>> getUnread(
            @Parameter(description = "Recipient user ID", example = "1")
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(notifService.getUnreadByRecipient(recipientId));
    }

    // ─── GET /notifications/user/{recipientId}/count ──────────────────────────

    @Operation(
        summary     = "Get unread notification count (badge count)",
        description = "Returns the count shown as a badge on the notification bell icon in the UI."
    )
    @ApiResponse(responseCode = "200", description = "Unread count returned")
    @GetMapping("/user/{recipientId}/count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @Parameter(description = "Recipient user ID", example = "1")
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(Map.of("unreadCount", notifService.getUnreadCount(recipientId)));
    }

    // ─── PUT /notifications/{notificationId}/read ─────────────────────────────

    @Operation(summary = "Mark a single notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @Parameter(description = "Notification ID", example = "10")
            @PathVariable Integer notificationId) {
        notifService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // ─── PUT /notifications/user/{recipientId}/readall ────────────────────────

    @Operation(
        summary     = "Mark all notifications as read",
        description = "Marks every notification for the user as read. Called when user opens notification panel."
    )
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    @PutMapping("/user/{recipientId}/readall")
    public ResponseEntity<Map<String, String>> markAllRead(
            @Parameter(description = "Recipient user ID", example = "1")
            @PathVariable Integer recipientId) {
        notifService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // ─── DELETE /notifications/{notificationId} ───────────────────────────────

    @Operation(summary = "Delete a notification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification deleted"),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> delete(
            @Parameter(description = "Notification ID", example = "10")
            @PathVariable Integer notificationId) {
        notifService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // ─── POST /notifications/email ────────────────────────────────────────────

    @Operation(
        summary     = "Send an email notification",
        description = "Sends an email for missed direct messages when the user is offline."
    )
    @ApiResponse(responseCode = "200", description = "Email sent")
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Email request payload",
                content     = @Content(
                    schema   = @Schema(implementation = EmailRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "to": "user@example.com",
                          "subject": "You have a new message",
                          "body": "John sent you a message: Hey, are you free?"
                        }
                        """)
                )
            )
            @RequestBody EmailRequest request) {
        notifService.sendEmail(request);
        return ResponseEntity.ok(Map.of("message", "Email sent"));
    }

    // ─── GET /notifications/all ───────────────────────────────────────────────

    @Operation(
        summary     = "Get all notifications (Admin only)",
        description = "Returns all notifications across all users. Intended for admin/monitoring use."
    )
    @ApiResponse(responseCode = "200", description = "All notifications returned")
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notifService.getAll());
    }
}
