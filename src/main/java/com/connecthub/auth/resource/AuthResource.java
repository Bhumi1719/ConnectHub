package com.connecthub.auth.resource;

import com.connecthub.auth.dto.*;
import com.connecthub.auth.entity.User;
import com.connecthub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Auth REST Controller.
 *
 * Swagger UI: http://localhost:8081/swagger-ui/index.html
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication & User Management",
    description = "Endpoints for user registration, login, JWT token management, " +
                  "profile updates, and user search."
)
public class AuthResource {

    private final AuthService authService;

    // ─── POST /auth/register ──────────────────────────────────────────────────

    @Operation(
        summary     = "Register a new user",
        description = "Creates a new local account and returns a JWT token on success."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registration successful — JWT token returned",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error — email or username already taken",
            content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody(
                description = "New user registration payload",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = RegisterRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "username": "johndoe",
                          "email": "john@example.com",
                          "password": "Secret@123",
                          "fullName": "John Doe"
                        }
                        """)
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ─── POST /auth/login ─────────────────────────────────────────────────────

    @Operation(
        summary     = "Login with email & password",
        description = "Authenticates a local user and returns a signed JWT Bearer token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody(
                description = "Email and password credentials",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = @ExampleObject(value = """
                        {
                          "email": "john@example.com",
                          "password": "Secret@123"
                        }
                        """)
                )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── POST /auth/logout/{userId} ───────────────────────────────────────────

    @Operation(
        summary     = "Logout user",
        description = "Records last-seen timestamp and sets status to INVISIBLE.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    @PostMapping("/logout/{userId}")
    public ResponseEntity<Map<String, String>> logout(
            @Parameter(description = "ID of the user to log out", required = true, example = "1")
            @PathVariable Integer userId) {
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ─── GET /auth/validate ───────────────────────────────────────────────────

    @Operation(
        summary     = "Validate a JWT token",
        description = "Used internally by the API Gateway to check if a token is valid."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns { valid: true/false }"),
        @ApiResponse(responseCode = "400", description = "Token parameter missing", content = @Content)
    })
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Parameter(description = "JWT token to validate", required = true)
            @RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", authService.validateToken(token)));
    }

    // ─── GET /auth/profile/{userId} ───────────────────────────────────────────

    @Operation(
        summary  = "Get user profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Integer userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // ─── PUT /auth/profile/{userId} ───────────────────────────────────────────

    @Operation(
        summary  = "Update user profile",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    // ─── PUT /auth/password/{userId} ──────────────────────────────────────────

    @Operation(
        summary  = "Change password",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed"),
        @ApiResponse(responseCode = "400", description = "Current password incorrect", content = @Content)
    })
    @PutMapping("/password/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ─── GET /auth/search ─────────────────────────────────────────────────────

    @Operation(
        summary  = "Search users by username/name",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "List of matching users")
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @Parameter(description = "Search keyword", example = "john")
            @RequestParam String keyword) {
        return ResponseEntity.ok(authService.searchUsers(keyword));
    }

    @Operation(
        summary  = "Get all active users",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Active users returned")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // ─── PUT /auth/status/{userId} ────────────────────────────────────────────

    @Operation(
        summary     = "Update online status",
        description = "Allowed values: ONLINE, AWAY, DND, INVISIBLE",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/status/{userId}")
    public ResponseEntity<Map<String, String>> updateStatus(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId,
            @org.springframework.web.bind.annotation.RequestBody UpdateStatusRequest request) {
        authService.updateStatus(userId, request.getStatus());
        return ResponseEntity.ok(Map.of("message", "Status updated to " + request.getStatus()));
    }

    // ─── PUT /auth/lastseen/{userId} ──────────────────────────────────────────

    @Operation(
        summary  = "Record last-seen timestamp",
        description = "Called by the WebSocket service on user disconnect.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/lastseen/{userId}")
    public ResponseEntity<Map<String, String>> recordLastSeen(
            @Parameter(description = "User ID", example = "1") @PathVariable Integer userId) {
        authService.recordLastSeen(userId);
        return ResponseEntity.ok(Map.of("message", "Last seen recorded"));
    }

    // ─── GET /auth/user/email ─────────────────────────────────────────────────

    @Operation(
        summary  = "Get user by email",
        description = "Used internally by other microservices to resolve a user from their email."
    )
    @GetMapping("/user/email")
    public ResponseEntity<User> getUserByEmail(
            @Parameter(description = "User email address", example = "john@example.com")
            @RequestParam String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }
}
