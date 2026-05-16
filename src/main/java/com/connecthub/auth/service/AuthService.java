package com.connecthub.auth.service;

import com.connecthub.auth.dto.*;
import com.connecthub.auth.entity.User;

import java.util.List;

public interface AuthService {

    // Register a new user, return JWT token
    AuthResponse register(RegisterRequest request);

    // Login with email + password, return JWT token
    AuthResponse login(LoginRequest request);

    // Logout — record lastSeenAt
    void logout(Integer userId);

    // Validate JWT token string
    boolean validateToken(String token);

    // Get user by ID
    User getUserById(Integer userId);

    // Get user by email
    User getUserByEmail(String email);

    // Get all active users
    List<User> getAllUsers();

    // Update profile (fullName, avatarUrl, bio)
    User updateProfile(Integer userId, UpdateProfileRequest request);

    // Change password (requires current password verification)
    void changePassword(Integer userId, ChangePasswordRequest request);

    // Search users by username keyword
    List<User> searchUsers(String keyword);

    // Update online status: ONLINE / AWAY / DND / INVISIBLE
    void updateStatus(Integer userId, String status);

    // Record lastSeenAt on WebSocket disconnect
    void recordLastSeen(Integer userId);
}
