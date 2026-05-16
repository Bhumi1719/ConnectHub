package com.connecthub.presence.resource;

import com.connecthub.presence.dto.BulkPresenceRequest;
import com.connecthub.presence.dto.SetOnlineRequest;
import com.connecthub.presence.dto.UpdateStatusRequest;
import com.connecthub.presence.entity.UserPresence;
import com.connecthub.presence.service.PresenceService;
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
import java.util.Optional;

@RestController
@RequestMapping("/presence")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name        = "Presence & Status",
    description = "Track user online/offline status, manual status updates (AWAY, DND), " +
                  "heartbeat pings, and bulk presence queries for room member lists."
)
public class PresenceResource {

    private final PresenceService presenceService;

    public PresenceResource(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    // ─── POST /presence/online ────────────────────────────────────────────────

    @Operation(
        summary     = "Set user online",
        description = "Called by WebSocket handler when a user connects. Creates or updates presence record."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User marked as ONLINE"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT missing or invalid", content = @Content)
    })
    @PostMapping("/online")
    public ResponseEntity<Map<String, String>> setOnline(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User and session details",
                required    = true,
                content     = @Content(
                    schema   = @Schema(implementation = SetOnlineRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "userId": 1,
                          "sessionId": "ws-session-abc123"
                        }
                        """)
                )
            )
            @Valid @RequestBody SetOnlineRequest request) {
        presenceService.setOnline(request);
        return ResponseEntity.ok(Map.of("message", "User is now ONLINE"));
    }

    // ─── PUT /presence/offline/{userId} ──────────────────────────────────────

    @Operation(
        summary     = "Set user offline",
        description = "Called by WebSocket handler on user disconnect. Records last-seen timestamp."
    )
    @ApiResponse(responseCode = "200", description = "User marked as OFFLINE")
    @PutMapping("/offline/{userId}")
    public ResponseEntity<Map<String, String>> setOffline(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Integer userId) {
        presenceService.setOffline(userId);
        return ResponseEntity.ok(Map.of("message", "User is now OFFLINE"));
    }

    // ─── PUT /presence/status/{userId} ───────────────────────────────────────

    @Operation(
        summary     = "Update user status manually",
        description = "Allows user to set their status. Allowed values: ONLINE, AWAY, DND, INVISIBLE."
    )
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/status/{userId}")
    public ResponseEntity<Map<String, String>> updateStatus(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "New status",
                content     = @Content(
                    schema   = @Schema(implementation = UpdateStatusRequest.class),
                    examples = @ExampleObject(value = """
                        { "status": "AWAY" }
                        """)
                )
            )
            @RequestBody UpdateStatusRequest request) {
        presenceService.updateStatus(userId, request);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
    }

    // ─── GET /presence/{userId} ───────────────────────────────────────────────

    @Operation(
        summary     = "Get presence for a single user",
        description = "Returns presence record if exists, otherwise returns `{ userId, status: OFFLINE }`."
    )
    @ApiResponse(responseCode = "200", description = "Presence data returned")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getPresence(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Integer userId) {
        Optional<UserPresence> presence = presenceService.getPresence(userId);
        if (presence.isPresent()) {
            return ResponseEntity.ok(presence.get());
        }
        return ResponseEntity.ok(Map.of("userId", userId, "status", "OFFLINE"));
    }

    // ─── POST /presence/bulk ─────────────────────────────────────────────────

    @Operation(
        summary     = "Get presence for multiple users",
        description = "Batch query used when loading a room's member list to show online indicators."
    )
    @ApiResponse(responseCode = "200", description = "Presence list returned")
    @PostMapping("/bulk")
    public ResponseEntity<List<UserPresence>> getBulkPresence(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "List of user IDs",
                content     = @Content(
                    schema   = @Schema(implementation = BulkPresenceRequest.class),
                    examples = @ExampleObject(value = """
                        { "userIds": [1, 2, 3, 5] }
                        """)
                )
            )
            @RequestBody BulkPresenceRequest request) {
        return ResponseEntity.ok(presenceService.getBulkPresence(request.getUserIds()));
    }

    // ─── GET /presence/online/all ─────────────────────────────────────────────

    @Operation(
        summary     = "Get all online users",
        description = "Returns list of all users currently online. Useful for admin dashboards."
    )
    @ApiResponse(responseCode = "200", description = "List of online users returned")
    @GetMapping("/online/all")
    public ResponseEntity<List<UserPresence>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsers());
    }

    // ─── GET /presence/online/count ───────────────────────────────────────────

    @Operation(summary = "Get total online user count")
    @ApiResponse(responseCode = "200", description = "Online count returned")
    @GetMapping("/online/count")
    public ResponseEntity<Map<String, Integer>> getOnlineCount() {
        return ResponseEntity.ok(Map.of("onlineCount", presenceService.getOnlineCount()));
    }

    // ─── PUT /presence/ping/{sessionId} ──────────────────────────────────────

    @Operation(
        summary     = "Heartbeat ping to keep session alive",
        description = "Called every 30 seconds by the client to prevent session expiry."
    )
    @ApiResponse(responseCode = "200", description = "Ping acknowledged")
    @PutMapping("/ping/{sessionId}")
    public ResponseEntity<Map<String, String>> ping(
            @Parameter(description = "WebSocket session ID", example = "ws-session-abc123")
            @PathVariable String sessionId) {
        presenceService.pingSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Ping received"));
    }

    // ─── GET /presence/isonline/{userId} ─────────────────────────────────────

    @Operation(
        summary     = "Quick check — is user online?",
        description = "Lightweight boolean check. Use this instead of /presence/{userId} when you only need online status."
    )
    @ApiResponse(responseCode = "200", description = "Online status returned")
    @GetMapping("/isonline/{userId}")
    public ResponseEntity<Map<String, Boolean>> isOnline(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Integer userId) {
        return ResponseEntity.ok(Map.of("isOnline", presenceService.isOnline(userId)));
    }
}
