package com.connecthub.room.repository;

import com.connecthub.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Integer> {

    List<RoomMember> findByRoomId(Integer roomId);

    List<RoomMember> findByUserId(Integer userId);

    Optional<RoomMember> findByRoomIdAndUserId(Integer roomId, Integer userId);

    boolean existsByRoomIdAndUserId(Integer roomId, Integer userId);

    int countByRoomId(Integer roomId);

    void deleteByRoomIdAndUserId(Integer roomId, Integer userId);

    // Count messages after lastReadAt for unread count
    @Query("SELECT COUNT(rm) FROM RoomMember rm WHERE rm.roomId = :roomId AND rm.userId = :userId AND rm.lastReadAt < :since")
    int countUnreadMessages(@Param("roomId") Integer roomId,
                            @Param("userId") Integer userId,
                            @Param("since") LocalDateTime since);
}
