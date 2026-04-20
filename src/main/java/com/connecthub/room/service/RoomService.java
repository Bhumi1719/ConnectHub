package com.connecthub.room.service;

import com.connecthub.room.dto.AddMemberRequest;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.entity.Room;
import com.connecthub.room.entity.RoomMember;

import java.util.List;

public interface RoomService {

    // Create GROUP room or DM
    Room createRoom(CreateRoomRequest request);

    // Get room by ID
    Room getRoomById(Integer roomId);

    // Get all rooms for a user
    List<Room> getRoomsByUser(Integer userId);

    // Update room settings
    Room updateRoom(Integer roomId, UpdateRoomRequest request);

    // Delete room
    void deleteRoom(Integer roomId);

    // Add member to room
    RoomMember addMember(Integer roomId, AddMemberRequest request);

    // Remove member from room
    void removeMember(Integer roomId, Integer userId);

    // Get all members of a room
    List<RoomMember> getMembers(Integer roomId);

    // Update member role (ADMIN / MEMBER)
    void updateMemberRole(Integer roomId, Integer userId, String role);

    // Mute or unmute a member
    void muteUnmuteMember(Integer roomId, Integer userId, boolean mute);

    // Update lastReadAt when user reads messages
    void updateLastRead(Integer roomId, Integer userId);

    // Get unread message count for a user in a room
    int getUnreadCount(Integer roomId, Integer userId);

    // Update lastMessageAt when new message arrives
    void updateLastMessageAt(Integer roomId);
}
