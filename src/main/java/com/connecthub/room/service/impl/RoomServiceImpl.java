package com.connecthub.room.service.impl;

import com.connecthub.room.dto.AddMemberRequest;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.RoomDirectoryEntry;
import com.connecthub.room.dto.RoomJoinRequestView;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.RoomJoinRequest;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.repository.RoomJoinRequestRepository;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import com.connecthub.room.service.RoomService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private static final Logger log = Logger.getLogger(RoomServiceImpl.class.getName());

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomJoinRequestRepository roomJoinRequestRepository;
    private final RestTemplate restTemplate;

    @Value("${message.service.url}")
    private String messageServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public RoomServiceImpl(RoomRepository roomRepository,
                           RoomMemberRepository roomMemberRepository,
                           RoomJoinRequestRepository roomJoinRequestRepository,
                           RestTemplate restTemplate) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomJoinRequestRepository = roomJoinRequestRepository;
        this.restTemplate = restTemplate;
    }

    // ─── Create Room ─────────────────────────────────────────────────────────

    @Override
    public Room createRoom(CreateRoomRequest request) {
        ensureUserExists(request.getCreatedById());

        // If DM — check if already exists
        if ("DM".equals(request.getType())) {
            if (request.getDmTargetUserId() == null) {
                throw new RuntimeException("dmTargetUserId is required for DM rooms");
            }
            ensureUserExists(request.getDmTargetUserId());
            roomRepository.findExistingDm(request.getCreatedById(), request.getDmTargetUserId())
                    .ifPresent(existing -> {
                        throw new RuntimeException("DM already exists with roomId: " + existing.getRoomId());
                    });
        }

        // Build room
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .createdById(request.getCreatedById())
                .avatarUrl(request.getAvatarUrl())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .maxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 100)
                .build();

        Room saved = roomRepository.save(room);
        log.info("Room created: " + saved.getRoomId() + " type=" + saved.getType());

        // Auto-add creator as ADMIN
        RoomMember creator = RoomMember.builder()
                .roomId(saved.getRoomId())
                .userId(request.getCreatedById())
                .role("ADMIN")
                .build();
        roomMemberRepository.save(creator);

        // For DM — auto-add target user as MEMBER
        if ("DM".equals(request.getType())) {
            RoomMember target = RoomMember.builder()
                    .roomId(saved.getRoomId())
                    .userId(request.getDmTargetUserId())
                    .role("MEMBER")
                    .build();
            roomMemberRepository.save(target);
        }

        return saved;
    }

    // ─── Get Room By ID ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));
    }

    // ─── Get Rooms By User ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByUser(Integer userId) {
        return roomRepository.findRoomsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDirectoryEntry> getRoomDirectory(Integer userId) {
        return roomRepository.findAll().stream()
                .filter(room -> !"DM".equals(room.getType()))
                .map(room -> {
                    boolean isJoined = roomMemberRepository.existsByRoomIdAndUserId(room.getRoomId(), userId);
                    String joinStatus = roomJoinRequestRepository
                            .findFirstByRoomIdAndUserIdOrderByCreatedAtDesc(room.getRoomId(), userId)
                            .map(RoomJoinRequest::getStatus)
                            .orElse(null);
                    Map<String, Object> creatorProfile = getProfile(room.getCreatedById());

                    return new RoomDirectoryEntry(
                            room.getRoomId(),
                            room.getName(),
                            room.getDescription(),
                            room.getCreatedById(),
                            profileValue(creatorProfile, "username"),
                            profileValue(creatorProfile, "fullName"),
                            isJoined,
                            joinStatus,
                            roomMemberRepository.countByRoomId(room.getRoomId())
                    );
                })
                .toList();
    }

    // ─── Update Room ──────────────────────────────────────────────────────────

    private Map<String, Object> getProfile(Integer userId) {
        if (userId == null) return null;
        try {
            return restTemplate.getForObject(
                    authServiceUrl + "/auth/profile/" + userId,
                    Map.class
            );
        } catch (Exception e) {
            log.warning("Could not load profile for userId=" + userId + ": " + e.getMessage());
            return null;
        }
    }

    private String profileValue(Map<String, Object> profile, String key) {
        if (profile == null || profile.get(key) == null) return "";
        return String.valueOf(profile.get(key));
    }

    @Override
    public Room updateRoom(Integer roomId, UpdateRoomRequest request) {
        Room room = getRoomById(roomId);
        if (request.getName() != null) room.setName(request.getName());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getAvatarUrl() != null) room.setAvatarUrl(request.getAvatarUrl());
        if (request.getMaxMembers() != null) room.setMaxMembers(request.getMaxMembers());
        Room updated = roomRepository.save(room);
        log.info("Room updated: " + roomId);
        return updated;
    }

    // ─── Delete Room ──────────────────────────────────────────────────────────

    @Override
    public void deleteRoom(Integer roomId, Integer requesterId) {
        Room room = getRoomById(roomId);
        List<RoomMember> members = roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId);

        if (requesterId == null) {
            roomJoinRequestRepository.deleteByRoomId(roomId);
            roomMemberRepository.deleteByRoomId(roomId);
            roomRepository.delete(room);
            log.info("Room deleted without requester: " + roomId);
            return;
        }

        RoomMember requester = members.stream()
                .filter(member -> requesterId.equals(member.getUserId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Requester is not a member of room " + roomId));

        if (!"ADMIN".equals(requester.getRole()) && !"OWNER".equals(requester.getRole())) {
            throw new RuntimeException("Only room admin can delete or transfer room");
        }

        if (members.size() <= 1) {
            roomJoinRequestRepository.deleteByRoomId(roomId);
            roomMemberRepository.deleteByRoomId(roomId);
            roomRepository.delete(room);
            log.info("Room deleted because admin was the last member: " + roomId);
            return;
        }

        RoomMember nextAdmin = members.stream()
                .filter(member -> !requesterId.equals(member.getUserId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No member available for admin transfer"));
        nextAdmin.setRole("ADMIN");
        roomMemberRepository.save(nextAdmin);
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, requesterId);
        log.info("Admin transferred to userId=" + nextAdmin.getUserId() + " for roomId=" + roomId);
    }

    // ─── Add Member ───────────────────────────────────────────────────────────

    @Override
    public RoomMember addMember(Integer roomId, AddMemberRequest request) {
        Room room = getRoomById(roomId);
        ensureUserExists(request.getUserId());

        // Check if already a member
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, request.getUserId())) {
            throw new RuntimeException("User " + request.getUserId() + " is already a member of room " + roomId);
        }

        // Check max members limit
        int currentCount = roomMemberRepository.countByRoomId(roomId);
        if (currentCount >= room.getMaxMembers()) {
            throw new RuntimeException("Room is full. Max members: " + room.getMaxMembers());
        }

        RoomMember member = RoomMember.builder()
                .roomId(roomId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : "MEMBER")
                .build();

        RoomMember saved = attachMemberProfile(roomMemberRepository.save(member));
        log.info("Member added userId=" + request.getUserId() + " to roomId=" + roomId);
        return saved;
    }

    @Override
    public RoomJoinRequest requestToJoin(Integer roomId, Integer userId) {
        getRoomById(roomId);
        ensureUserExists(userId);

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("User " + userId + " is already a member of room " + roomId);
        }

        return roomJoinRequestRepository.findByRoomIdAndUserIdAndStatus(roomId, userId, "PENDING")
                .orElseGet(() -> {
                    RoomJoinRequest request = new RoomJoinRequest();
                    request.setRoomId(roomId);
                    request.setUserId(userId);
                    request.setStatus("PENDING");
                    return roomJoinRequestRepository.save(request);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomJoinRequestView> getPendingJoinRequests(Integer roomId) {
        getRoomById(roomId);
        return roomJoinRequestRepository.findByRoomIdAndStatusOrderByCreatedAtAsc(roomId, "PENDING")
                .stream()
                .map(this::toJoinRequestView)
                .toList();
    }

    @Override
    public RoomMember approveJoinRequest(Integer roomId, Integer userId) {
        RoomJoinRequest request = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, userId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Pending join request not found"));

        AddMemberRequest addMemberRequest = new AddMemberRequest();
        addMemberRequest.setUserId(userId);
        addMemberRequest.setRole("MEMBER");

        RoomMember member = addMember(roomId, addMemberRequest);
        request.setStatus("ACCEPTED");
        request.setDecidedAt(LocalDateTime.now());
        roomJoinRequestRepository.save(request);
        return member;
    }

    @Override
    public RoomJoinRequest rejectJoinRequest(Integer roomId, Integer userId) {
        RoomJoinRequest request = roomJoinRequestRepository
                .findByRoomIdAndUserIdAndStatus(roomId, userId, "PENDING")
                .orElseThrow(() -> new RuntimeException("Pending join request not found"));

        request.setStatus("REJECTED");
        request.setDecidedAt(LocalDateTime.now());
        return roomJoinRequestRepository.save(request);
    }

    private RoomJoinRequestView toJoinRequestView(RoomJoinRequest request) {
        Map<String, Object> profile = null;

        try {
            profile = restTemplate.getForObject(
                    authServiceUrl + "/auth/profile/" + request.getUserId(),
                    Map.class
            );
        } catch (Exception e) {
            log.warning("Could not load join request profile for userId=" + request.getUserId()
                    + ": " + e.getMessage());
        }

        return new RoomJoinRequestView(
                request.getId(),
                request.getRoomId(),
                request.getUserId(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                profile != null ? String.valueOf(profile.getOrDefault("username", "")) : "",
                profile != null ? String.valueOf(profile.getOrDefault("fullName", "")) : "",
                profile != null ? String.valueOf(profile.getOrDefault("avatarUrl", "")) : ""
        );
    }

    // ─── Remove Member ────────────────────────────────────────────────────────

    @Override
    public void removeMember(Integer roomId, Integer userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("User " + userId + " is not a member of room " + roomId));

        if ("ADMIN".equals(member.getRole()) && roomMemberRepository.countByRoomId(roomId) == 1) {
            deleteRoom(roomId, userId);
            log.info("Room deleted because last admin left roomId=" + roomId);
            return;
        }

        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
        log.info("Member removed userId=" + userId + " from roomId=" + roomId);
    }

    // ─── Get Members ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RoomMember> getMembers(Integer roomId) {
        return roomMemberRepository.findByRoomIdOrderByJoinedAtAsc(roomId)
                .stream()
                .filter(member -> userExists(member.getUserId()))
                .map(this::attachMemberProfile)
                .toList();
    }

    private RoomMember attachMemberProfile(RoomMember member) {
        Map<String, Object> profile = getProfile(member.getUserId());
        if (profile != null) {
            member.setUsername(profileValue(profile, "username"));
            member.setEmail(profileValue(profile, "email"));
            member.setFullName(profileValue(profile, "fullName"));
            member.setAvatarUrl(profileValue(profile, "avatarUrl"));
        }
        return member;
    }

    // ─── Update Member Role ───────────────────────────────────────────────────

    @Override
    public void updateMemberRole(Integer roomId, Integer userId, String role) {
        List<String> allowed = List.of("ADMIN", "MEMBER");
        if (!allowed.contains(role)) {
            throw new RuntimeException("Invalid role. Allowed: ADMIN, MEMBER");
        }
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setRole(role);
        roomMemberRepository.save(member);
        log.info("Role updated to " + role + " for userId=" + userId + " in roomId=" + roomId);
    }

    // ─── Mute / Unmute Member ─────────────────────────────────────────────────

    @Override
    public void muteUnmuteMember(Integer roomId, Integer userId, boolean mute) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setIsMuted(mute);
        roomMemberRepository.save(member);
        log.info("User " + userId + " muted=" + mute + " in roomId=" + roomId);
    }

    // ─── Update Last Read ─────────────────────────────────────────────────────
    // Called when READ_RECEIPT STOMP event comes

    @Override
    public void updateLastRead(Integer roomId, Integer userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setLastReadAt(LocalDateTime.now());
        roomMemberRepository.save(member);
    }

    // ─── Get Unread Count ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Integer roomId, Integer userId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElse(null);
        if (member == null) return 0;

        LocalDateTime since = member.getLastReadAt() != null
                ? member.getLastReadAt()
                : member.getJoinedAt();

        if (since == null) return 0;

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(messageServiceUrl + "/messages/room/" + roomId + "/unread-count")
                    .queryParam("userId", userId)
                    .queryParam("since", since)
                    .toUriString();
            Map response = restTemplate.getForObject(url, Map.class);
            Object unreadCount = response != null ? response.get("unreadCount") : null;
            return unreadCount instanceof Number ? ((Number) unreadCount).intValue() : 0;
        } catch (Exception e) {
            log.warning("Could not fetch unread count for roomId=" + roomId
                    + " userId=" + userId + ": " + e.getMessage());
            return 0;
        }
    }

    // ─── Update Last Message At ───────────────────────────────────────────────
    // Called by Message Service when new message is sent

    @Override
    public void updateLastMessageAt(Integer roomId) {
        Room room = getRoomById(roomId);
        room.setLastMessageAt(LocalDateTime.now());
        roomRepository.save(room);
    }

    private void ensureUserExists(Integer userId) {
        if (!userExists(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
    }

    private boolean userExists(Integer userId) {
        if (userId == null) {
            return false;
        }
        try {
            Map profile = restTemplate.getForObject(authServiceUrl + "/auth/profile/" + userId, Map.class);
            return profile != null && profile.get("userId") != null;
        } catch (Exception e) {
            log.warning("Could not validate userId=" + userId + ": " + e.getMessage());
            return false;
        }
    }
}
