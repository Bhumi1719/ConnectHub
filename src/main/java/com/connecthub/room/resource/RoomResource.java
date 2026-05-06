package com.connecthub.room.resource;

import com.connecthub.room.dto.AddMemberRequest;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.RoomDirectoryEntry;
import com.connecthub.room.dto.RoomJoinRequestView;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.RoomJoinRequest;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.service.RoomService;
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
@RequestMapping("/rooms")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name        = "Room Management",
    description = "Chat room operations — create rooms, manage members, track unread counts and last-read timestamps."
)
public class RoomResource {

    private final RoomService roomService;

    public RoomResource(RoomService roomService) {
        this.roomService = roomService;
    }

    // ─── POST /rooms ──────────────────────────────────────────────────────────

    @Operation(
        summary     = "Create a new room",
        description = "Creates a GROUP or DIRECT room. The creator is automatically set as owner."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = Room.class))),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT token missing or invalid", content = @Content)
    })
    @PostMapping
    public ResponseEntity<Room> createRoom(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Room creation payload",
                required    = true,
                content     = @Content(
                    schema   = @Schema(implementation = CreateRoomRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "name": "Engineering Team",
                          "type": "GROUP",
                          "createdBy": 1
                        }
                        """)
                )
            )
            @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    // ─── GET /rooms/{roomId} ──────────────────────────────────────────────────

    @Operation(summary = "Get room by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room found",
            content = @Content(schema = @Schema(implementation = Room.class))),
        @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoomById(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    // ─── GET /rooms/user/{userId} ─────────────────────────────────────────────

    @Operation(
        summary     = "Get all rooms for a user",
        description = "Returns every room the user is a member of, sorted by last message time."
    )
    @ApiResponse(responseCode = "200", description = "List of rooms returned")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Room>> getRoomsByUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Integer userId) {
        return ResponseEntity.ok(roomService.getRoomsByUser(userId));
    }

    @Operation(summary = "Get all group rooms with current user's join status")
    @ApiResponse(responseCode = "200", description = "Room directory returned")
    @GetMapping
    public ResponseEntity<List<RoomDirectoryEntry>> getRoomDirectory(
            @Parameter(description = "Current User ID", example = "1")
            @RequestParam Integer userId) {
        return ResponseEntity.ok(roomService.getRoomDirectory(userId));
    }

    // ─── PUT /rooms/{roomId} ──────────────────────────────────────────────────

    @Operation(summary = "Update room details (name, avatar, description)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room updated",
            content = @Content(schema = @Schema(implementation = Room.class))),
        @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    })
    @PutMapping("/{roomId}")
    public ResponseEntity<Room> updateRoom(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Fields to update",
                content     = @Content(
                    schema   = @Schema(implementation = UpdateRoomRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "name": "Updated Room Name",
                          "avatarUrl": "https://example.com/avatar.png"
                        }
                        """)
                )
            )
            @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request));
    }

    // ─── DELETE /rooms/{roomId} ───────────────────────────────────────────────

    @Operation(
        summary     = "Delete a room",
        description = "Permanently deletes the room and all its members. Only room owner can delete."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Room deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Only room owner can delete", content = @Content),
        @ApiResponse(responseCode = "404", description = "Room not found", content = @Content)
    })
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Map<String, String>> deleteRoom(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
    }

    // ─── POST /rooms/{roomId}/members ─────────────────────────────────────────

    @Operation(summary = "Add a member to a room")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member added",
            content = @Content(schema = @Schema(implementation = RoomMember.class))),
        @ApiResponse(responseCode = "400", description = "User already a member", content = @Content)
    })
    @PostMapping("/{roomId}/members")
    public ResponseEntity<RoomMember> addMember(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Member to add",
                content     = @Content(
                    schema   = @Schema(implementation = AddMemberRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "userId": 5,
                          "role": "MEMBER"
                        }
                        """)
                )
            )
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(roomService.addMember(roomId, request));
    }

    @Operation(summary = "Request to join a room")
    @ApiResponse(responseCode = "200", description = "Join request created")
    @PostMapping("/{roomId}/request")
    public ResponseEntity<RoomJoinRequest> requestToJoin(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(roomService.requestToJoin(roomId, body.get("userId")));
    }

    @Operation(summary = "Get pending join requests for a room")
    @ApiResponse(responseCode = "200", description = "Pending join requests returned")
    @GetMapping("/{roomId}/requests")
    public ResponseEntity<List<RoomJoinRequestView>> getPendingJoinRequests(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getPendingJoinRequests(roomId));
    }

    @Operation(summary = "Approve a room join request")
    @ApiResponse(responseCode = "200", description = "Join request approved")
    @PostMapping("/{roomId}/approve/{userId}")
    public ResponseEntity<RoomMember> approveJoinRequest(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "5") @PathVariable Integer userId) {
        return ResponseEntity.ok(roomService.approveJoinRequest(roomId, userId));
    }

    @Operation(summary = "Reject a room join request")
    @ApiResponse(responseCode = "200", description = "Join request rejected")
    @PostMapping("/{roomId}/reject/{userId}")
    public ResponseEntity<RoomJoinRequest> rejectJoinRequest(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "5") @PathVariable Integer userId) {
        return ResponseEntity.ok(roomService.rejectJoinRequest(roomId, userId));
    }

    // ─── DELETE /rooms/{roomId}/members/{userId} ──────────────────────────────

    @Operation(summary = "Remove a member from a room")
    @ApiResponse(responseCode = "200", description = "Member removed successfully")
    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Map<String, String>> removeMember(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID to remove", example = "5") @PathVariable Integer userId) {
        roomService.removeMember(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    // ─── GET /rooms/{roomId}/members ──────────────────────────────────────────

    @Operation(summary = "Get all members of a room")
    @ApiResponse(responseCode = "200", description = "Member list returned")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<List<RoomMember>> getMembers(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId));
    }

    // ─── PUT /rooms/{roomId}/members/{userId}/role ────────────────────────────

    @Operation(
        summary     = "Update member role",
        description = "Allowed roles: OWNER, ADMIN, MEMBER"
    )
    @ApiResponse(responseCode = "200", description = "Role updated")
    @PutMapping("/{roomId}/members/{userId}/role")
    public ResponseEntity<Map<String, String>> updateRole(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "5") @PathVariable Integer userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "New role",
                content     = @Content(examples = @ExampleObject(value = """
                    { "role": "ADMIN" }
                    """))
            )
            @RequestBody Map<String, String> body) {
        roomService.updateMemberRole(roomId, userId, body.get("role"));
        return ResponseEntity.ok(Map.of("message", "Role updated to " + body.get("role")));
    }

    // ─── PUT /rooms/{roomId}/members/{userId}/mute ────────────────────────────

    @Operation(
        summary     = "Mute or unmute a member",
        description = "Pass `mute: true` to mute, `mute: false` to unmute."
    )
    @ApiResponse(responseCode = "200", description = "Mute status updated")
    @PutMapping("/{roomId}/members/{userId}/mute")
    public ResponseEntity<Map<String, String>> muteUnmute(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "5") @PathVariable Integer userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Mute flag",
                content     = @Content(examples = @ExampleObject(value = """
                    { "mute": true }
                    """))
            )
            @RequestBody Map<String, Boolean> body) {
        boolean mute = body.getOrDefault("mute", true);
        roomService.muteUnmuteMember(roomId, userId, mute);
        return ResponseEntity.ok(Map.of("message", "Mute updated: " + mute));
    }

    // ─── PUT /rooms/{roomId}/lastread/{userId} ────────────────────────────────

    @Operation(
        summary     = "Update last-read timestamp for a user in a room",
        description = "Called by WebSocket service on READ_RECEIPT event."
    )
    @ApiResponse(responseCode = "200", description = "Last read timestamp updated")
    @PutMapping("/{roomId}/lastread/{userId}")
    public ResponseEntity<Map<String, String>> updateLastRead(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId) {
        roomService.updateLastRead(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Last read updated"));
    }

    // ─── GET /rooms/{roomId}/unread/{userId} ──────────────────────────────────

    @Operation(summary = "Get unread message count for a user in a room")
    @ApiResponse(responseCode = "200", description = "Unread count returned")
    @GetMapping("/{roomId}/unread/{userId}")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @Parameter(description = "Room ID", example = "1") @PathVariable Integer roomId,
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId) {
        return ResponseEntity.ok(Map.of("unreadCount", roomService.getUnreadCount(roomId, userId)));
    }

    // ─── PUT /rooms/{roomId}/lastmessage ──────────────────────────────────────

    @Operation(
        summary     = "Update room's last message timestamp",
        description = "Called internally by Message Service when a new message is sent."
    )
    @ApiResponse(responseCode = "200", description = "Last message time updated")
    @PutMapping("/{roomId}/lastmessage")
    public ResponseEntity<Map<String, String>> updateLastMessage(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        roomService.updateLastMessageAt(roomId);
        return ResponseEntity.ok(Map.of("message", "Last message time updated"));
    }
}
