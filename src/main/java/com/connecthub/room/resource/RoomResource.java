package com.connecthub.room.resource;

import com.connecthub.room.dto.AddMemberRequest;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
public class RoomResource {

    private final RoomService roomService;

    public RoomResource(RoomService roomService) {
        this.roomService = roomService;
    }

    // ─── POST /rooms ──────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Room> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request);
        return ResponseEntity.ok(room);
    }

    // ─── GET /rooms/{roomId} ──────────────────────────────────────────────────
    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoomById(@PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    // ─── GET /rooms/user/{userId} ─────────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Room>> getRoomsByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(roomService.getRoomsByUser(userId));
    }

    // ─── PUT /rooms/{roomId} ──────────────────────────────────────────────────
    @PutMapping("/{roomId}")
    public ResponseEntity<Room> updateRoom(@PathVariable Integer roomId,
                                           @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    // ─── DELETE /rooms/{roomId} ───────────────────────────────────────────────
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable Integer roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
    }

    // ─── POST /rooms/{roomId}/members ─────────────────────────────────────────
    @PostMapping("/{roomId}/members")
    public ResponseEntity<RoomMember> addMember(@PathVariable Integer roomId,
                                                @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(roomService.addMember(roomId, request));
    }

    // ─── DELETE /rooms/{roomId}/members/{userId} ──────────────────────────────
    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Map<String, String>> removeMember(@PathVariable Integer roomId,
                                                            @PathVariable Integer userId) {
        roomService.removeMember(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    // ─── GET /rooms/{roomId}/members ──────────────────────────────────────────
    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<RoomMember>> getMembers(@PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId));
    }

    // ─── PUT /rooms/{roomId}/members/{userId}/role ────────────────────────────
    @PutMapping("/{roomId}/members/{userId}/role")
    public ResponseEntity<Map<String, String>> updateRole(@PathVariable Integer roomId,
                                                          @PathVariable Integer userId,
                                                          @RequestBody Map<String, String> body) {
        roomService.updateMemberRole(roomId, userId, body.get("role"));
        return ResponseEntity.ok(Map.of("message", "Role updated to " + body.get("role")));
    }

    // ─── PUT /rooms/{roomId}/members/{userId}/mute ────────────────────────────
    @PutMapping("/{roomId}/members/{userId}/mute")
    public ResponseEntity<Map<String, String>> muteUnmute(@PathVariable Integer roomId,
                                                          @PathVariable Integer userId,
                                                          @RequestBody Map<String, Boolean> body) {
        boolean mute = body.getOrDefault("mute", true);
        roomService.muteUnmuteMember(roomId, userId, mute);
        return ResponseEntity.ok(Map.of("message", "Mute updated: " + mute));
    }

    // ─── PUT /rooms/{roomId}/lastread/{userId} ────────────────────────────────
    // Called by WebSocket Handler on READ_RECEIPT event
    @PutMapping("/{roomId}/lastread/{userId}")
    public ResponseEntity<Map<String, String>> updateLastRead(@PathVariable Integer roomId,
                                                              @PathVariable Integer userId) {
        roomService.updateLastRead(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Last read updated"));
    }

    // ─── GET /rooms/{roomId}/unread/{userId} ──────────────────────────────────
    @GetMapping("/{roomId}/unread/{userId}")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable Integer roomId,
                                                               @PathVariable Integer userId) {
        int count = roomService.getUnreadCount(roomId, userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ─── PUT /rooms/{roomId}/lastmessage ──────────────────────────────────────
    // Called by Message Service when new message arrives
    @PutMapping("/{roomId}/lastmessage")
    public ResponseEntity<Map<String, String>> updateLastMessage(@PathVariable Integer roomId) {
        roomService.updateLastMessageAt(roomId);
        return ResponseEntity.ok(Map.of("message", "Last message time updated"));
    }
}
