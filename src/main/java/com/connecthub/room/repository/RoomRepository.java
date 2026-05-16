package com.connecthub.room.repository;

import com.connecthub.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    Optional<Room> findByRoomId(Integer roomId);

    List<Room> findByCreatedById(Integer createdById);

    List<Room> findByType(String type);

    // Get all rooms a user belongs to (via RoomMember join)
    @Query("SELECT r FROM Room r JOIN RoomMember rm ON r.roomId = rm.roomId WHERE rm.userId = :userId ORDER BY r.lastMessageAt DESC")
    List<Room> findRoomsByUserId(@Param("userId") Integer userId);

    // Check if DM already exists between 2 users
    @Query("SELECT r FROM Room r JOIN RoomMember rm1 ON r.roomId = rm1.roomId JOIN RoomMember rm2 ON r.roomId = rm2.roomId WHERE r.type = 'DM' AND rm1.userId = :userId1 AND rm2.userId = :userId2")
    Optional<Room> findExistingDm(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
}
