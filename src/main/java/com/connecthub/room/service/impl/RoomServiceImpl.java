package com.connecthub.room.service.impl;

import com.connecthub.room.dto.AddMemberRequest;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.RoomMember;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import com.connecthub.room.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private static final Logger log = Logger.getLogger(RoomServiceImpl.class.getName());

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    public RoomServiceImpl(RoomRepository roomRepository,
                           RoomMemberRepository roomMemberRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
    }

    // ─── Create Room ─────────────────────────────────────────────────────────

    @Override
    public Room createRoom(CreateRoomRequest request) {

        // If DM — check if already exists
        if ("DM".equals(request.getType())) {
            if (request.getDmTargetUserId() == null) {
                throw new RuntimeException("dmTargetUserId is required for DM rooms");
            }
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

    // ─── Update Room ──────────────────────────────────────────────────────────

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
    public void deleteRoom(Integer roomId) {
        Room room = getRoomById(roomId);
        roomRepository.delete(room);
        log.info("Room deleted: " + roomId);
    }

    // ─── Add Member ───────────────────────────────────────────────────────────

    @Override
    public RoomMember addMember(Integer roomId, AddMemberRequest request) {
        Room room = getRoomById(roomId);

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

        RoomMember saved = roomMemberRepository.save(member);
        log.info("Member added userId=" + request.getUserId() + " to roomId=" + roomId);
        return saved;
    }

    // ─── Remove Member ────────────────────────────────────────────────────────

    @Override
    public void removeMember(Integer roomId, Integer userId) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("User " + userId + " is not a member of room " + roomId);
        }
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
        log.info("Member removed userId=" + userId + " from roomId=" + roomId);
    }

    // ─── Get Members ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RoomMember> getMembers(Integer roomId) {
        return roomMemberRepository.findByRoomId(roomId);
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
        if (member == null || member.getLastReadAt() == null) return 0;
        return roomMemberRepository.countUnreadMessages(roomId, userId, member.getLastReadAt());
    }

    // ─── Update Last Message At ───────────────────────────────────────────────
    // Called by Message Service when new message is sent

    @Override
    public void updateLastMessageAt(Integer roomId) {
        Room room = getRoomById(roomId);
        room.setLastMessageAt(LocalDateTime.now());
        roomRepository.save(room);
    }
}
