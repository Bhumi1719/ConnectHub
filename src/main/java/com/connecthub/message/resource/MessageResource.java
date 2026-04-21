package com.connecthub.message.resource;

import com.connecthub.message.dto.EditMessageRequest;
import com.connecthub.message.dto.SendMessageRequest;
import com.connecthub.message.dto.UpdateDeliveryStatusRequest;
import com.connecthub.message.entity.Message;
import com.connecthub.message.service.MessageService;
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
public class MessageResource {

    private final MessageService messageService;

    public MessageResource(MessageService messageService) {
        this.messageService = messageService;
    }

    // ─── POST /messages ───────────────────────────────────────────────────────
    // Send a new message
    @PostMapping
    public ResponseEntity<Message> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendMessage(request);
        return ResponseEntity.ok(message);
    }

    // ─── GET /messages/{messageId} ────────────────────────────────────────────
    @GetMapping("/{messageId}")
    public ResponseEntity<Message> getMessageById(@PathVariable Integer messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    // ─── GET /messages/room/{roomId}?page=0&size=20 ───────────────────────────
    // Paginated messages for infinite scroll (newest first)
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<Message>> getMessagesByRoom(
            @PathVariable Integer roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(messageService.getMessagesByRoom(roomId, page, size));
    }

    // ─── GET /messages/room/{roomId}/before?time=xxx ──────────────────────────
    // Load older messages (for scroll up)
    @GetMapping("/room/{roomId}/before")
    public ResponseEntity<List<Message>> getMessagesBefore(
            @PathVariable Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return ResponseEntity.ok(messageService.getMessagesBefore(roomId, time));
    }

    // ─── PUT /messages/{messageId} ────────────────────────────────────────────
    // Edit message content
    @PutMapping("/{messageId}")
    public ResponseEntity<Message> editMessage(
            @PathVariable Integer messageId,
            @Valid @RequestBody EditMessageRequest request) {
        return ResponseEntity.ok(messageService.editMessage(messageId, request));
    }

    // ─── DELETE /messages/{messageId} ────────────────────────────────────────
    // Soft delete
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable Integer messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
    }

    // ─── GET /messages/room/{roomId}/search?keyword=hello ────────────────────
    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<List<Message>> searchMessages(
            @PathVariable Integer roomId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(messageService.searchMessages(roomId, keyword));
    }

    // ─── PUT /messages/{messageId}/status ────────────────────────────────────
    // Update delivery status: SENT → DELIVERED → READ
    // Called by WebSocket Handler
    @PutMapping("/{messageId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Integer messageId,
            @RequestBody UpdateDeliveryStatusRequest request) {
        messageService.updateDeliveryStatus(messageId, request.getStatus());
        return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
    }

    // ─── GET /messages/room/{roomId}/count ───────────────────────────────────
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Long>> getMessageCount(@PathVariable Integer roomId) {
        long count = messageService.getMessageCount(roomId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ─── GET /messages/room/{roomId}/unread?since=xxx ────────────────────────
    // Get unread messages since lastReadAt
    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<List<Message>> getUnreadMessages(
            @PathVariable Integer roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(messageService.getUnreadMessages(roomId, since));
    }
}
