package com.connecthub.message.resource;

import com.connecthub.message.dto.EditMessageRequest;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.dto.UpdateDeliveryStatusRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.service.MessageService;
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
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name        = "Message Management",
    description = "Send, edit, delete and retrieve messages. Update delivery/read status. " +
                  "Supports pagination for infinite scroll."
)
public class MessageResource {

    private final MessageService messageService;

    public MessageResource(MessageService messageService) {
        this.messageService = messageService;
    }

    // ─── POST /messages ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Send a new message",
        description = "Sends a message to a room. Supports TEXT, IMAGE, FILE, and REACTION types."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = Message.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT token missing or invalid", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Message> sendMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Message payload",
                required    = true,
                content     = @Content(
                    schema   = @Schema(implementation = SendMessageRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "roomId": 1,
                          "senderId": 1,
                          "content": "Hello team!",
                          "messageType": "TEXT"
                        }
                        """)
                )
            )
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.sendMessage(request));
    }

    // ─── GET /messages/{messageId} ────────────────────────────────────────────

    @Operation(summary = "Get a single message by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message found",
            content = @Content(schema = @Schema(implementation = Message.class))),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    @GetMapping("/{messageId}")
    public ResponseEntity<Message> getMessageById(
            @Parameter(description = "Message ID", example = "100")
            @PathVariable Integer messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    // ─── GET /messages/room/{roomId}?page=0&size=20 ───────────────────────────

    @Operation(
        summary     = "Get paginated messages for a room",
        description = "Returns messages newest-first for infinite scroll. Default: page=0, size=20."
    )
    @ApiResponse(responseCode = "200", description = "Paginated message list returned")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<Message>> getMessagesByRoom(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "Page number (0-based)", example = "0")
                @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(messageService.getMessagesByRoom(roomId, page, size));
    }

    // ─── GET /messages/room/{roomId}/before?time=xxx ──────────────────────────

    @Operation(
        summary     = "Load older messages before a timestamp",
        description = "Used for scroll-up / load more. Returns messages older than the given time. " +
                      "Format: `2024-01-15T10:30:00`"
    )
    @ApiResponse(responseCode = "200", description = "Older messages returned")
    @GetMapping("/room/{roomId}/before")
    public ResponseEntity<List<Message>> getMessagesBefore(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "Load messages before this time (ISO format)", example = "2024-01-15T10:30:00")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return ResponseEntity.ok(messageService.getMessagesBefore(roomId, time));
    }

    // ─── PUT /messages/{messageId} ────────────────────────────────────────────

    @Operation(
        summary     = "Edit a message",
        description = "Updates the message content. Sets `isEdited = true` and records edit timestamp."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message edited",
            content = @Content(schema = @Schema(implementation = Message.class))),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(
            @Parameter(description = "Message ID", example = "100") @PathVariable Integer messageId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated content",
                content     = @Content(
                    schema   = @Schema(implementation = EditMessageRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "content": "Updated message content"
                        }
                        """)
                )
            )
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request));
    }

    // ─── DELETE /messages/{messageId} ─────────────────────────────────────────

    @Operation(
        summary     = "Delete a message (soft delete)",
        description = "Marks the message as deleted. Content is replaced with 'This message was deleted'."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message deleted"),
        @ApiResponse(responseCode = "404", description = "Message not found", content = @Content)
    })
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(
            @Parameter(description = "Message ID", example = "100")
            @PathVariable Integer messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
    }

    // ─── GET /messages/room/{roomId}/search?keyword=hello ────────────────────

    @Operation(
        summary     = "Search messages in a room by keyword",
        description = "Full-text search within a room's message history."
    )
    @ApiResponse(responseCode = "200", description = "Matching messages returned")
    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<List<Message>> searchMessages(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "Search keyword", example = "hello") @RequestParam String keyword) {
        return ResponseEntity.ok(messageService.searchMessages(roomId, keyword));
    }

    // ─── PUT /messages/{messageId}/status ─────────────────────────────────────

    @Operation(
        summary     = "Update message delivery status",
        description = "Transitions: SENT → DELIVERED → READ. Called by WebSocket handler."
    )
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/{messageId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @Parameter(description = "Message ID", example = "100") @PathVariable Integer messageId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "New delivery status",
                content     = @Content(
                    schema   = @Schema(implementation = UpdateDeliveryStatusRequest.class),
                    examples = @ExampleObject(value = """
                        { "status": "READ" }
                        """)
                )
            )
            @RequestBody UpdateDeliveryStatusRequest request) {
        messageService.updateDeliveryStatus(messageId, request.getStatus());
        return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
    }

    // ─── GET /messages/room/{roomId}/count ───────────────────────────────────

    @Operation(summary = "Get total message count in a room")
    @ApiResponse(responseCode = "200", description = "Message count returned")
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Long>> getMessageCount(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(Map.of("count", messageService.getMessageCount(roomId)));
    }

    // ─── GET /messages/room/{roomId}/unread?since=xxx ────────────────────────

    @Operation(
        summary     = "Get unread messages since a given timestamp",
        description = "Used on reconnect to fetch missed messages. Pass lastReadAt timestamp."
    )
    @ApiResponse(responseCode = "200", description = "Unread messages returned")
    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<List<Message>> getUnreadMessages(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "Last read timestamp (ISO format)", example = "2024-01-15T10:30:00")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(messageService.getUnreadMessages(roomId, since));
    }

    @Operation(summary = "Get unread message count for a user since a timestamp")
    @ApiResponse(responseCode = "200", description = "Unread count returned")
    @GetMapping("/room/{roomId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "Current user ID", example = "7") @RequestParam Integer userId,
            @Parameter(description = "Last read timestamp (ISO format)", example = "2024-01-15T10:30:00")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(Map.of("unreadCount", messageService.getUnreadCount(roomId, userId, since)));
    }
}
