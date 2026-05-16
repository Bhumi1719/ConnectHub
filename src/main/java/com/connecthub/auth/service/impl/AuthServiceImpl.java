package com.connecthub.auth.service.impl;

import com.connecthub.auth.config.JwtConfig;
import com.connecthub.auth.dto.*;
import com.connecthub.auth.entity.User;
import com.connecthub.auth.repository.UserRepository;
import com.connecthub.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    // ─── Register ────────────────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check duplicates
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken: " + request.getUsername());
        }

        // Build and save user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .status("ONLINE")
                .provider("LOCAL")
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        String token = jwtConfig.generateToken(saved.getUserId(), saved.getEmail());
        return buildAuthResponse(token, saved);
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Account is suspended. Contact admin.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Set status to ONLINE on login
        user.setStatus("ONLINE");
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        String token = jwtConfig.generateToken(user.getUserId(), user.getEmail());
        return buildAuthResponse(token, user);
    }

    // ─── Logout ──────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "user", key = "#userId")                // ✅ evict user cache on logout
    public void logout(Integer userId) {
        recordLastSeen(userId);
        log.info("User logged out, userId: {}", userId);
    }

    // ─── Validate Token ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return jwtConfig.validateToken(token);
    }

    // ─── Get User ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#userId")                 // ✅ cache user by id
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "user", key = "#email")                  // ✅ cache user by email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findActiveUsers();
    }

    // ─── Update Profile ──────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "user", key = "#userId")                // ✅ evict stale user on profile update
    public User updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBio() != null) user.setBio(request.getBio());

        User updated = userRepository.save(user);
        log.info("Profile updated for userId: {}", userId);
        return updated;
    }

    // ─── Change Password ─────────────────────────────────────────────────────

    @Override
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for userId: {}", userId);
    }

    // ─── Search Users ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        return userRepository.searchByUsername(keyword);
    }

    // ─── Update Status ───────────────────────────────────────────────────────

    @Override
    public void updateStatus(Integer userId, String status) {
        // Validate allowed statuses
        List<String> allowed = List.of("ONLINE", "AWAY", "DND", "INVISIBLE");
        if (!allowed.contains(status)) {
            throw new RuntimeException("Invalid status. Allowed: ONLINE, AWAY, DND, INVISIBLE");
        }
        User user = getUserById(userId);
        user.setStatus(status);
        userRepository.save(user);
        log.info("Status updated to {} for userId: {}", status, userId);
    }

    // ─── Record Last Seen ────────────────────────────────────────────────────

    @Override
    public void recordLastSeen(Integer userId) {
        User user = getUserById(userId);
        user.setLastSeenAt(LocalDateTime.now());
        user.setStatus("INVISIBLE");
        userRepository.save(user);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
