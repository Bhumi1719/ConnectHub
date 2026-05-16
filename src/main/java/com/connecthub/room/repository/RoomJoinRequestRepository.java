package com.connecthub.room.repository;

import com.connecthub.room.entity.RoomJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomJoinRequestRepository extends JpaRepository<RoomJoinRequest, Integer> {
    List<RoomJoinRequest> findByRoomIdAndStatusOrderByCreatedAtAsc(Integer roomId, String status);

    Optional<RoomJoinRequest> findByRoomIdAndUserIdAndStatus(Integer roomId, Integer userId, String status);

    Optional<RoomJoinRequest> findFirstByRoomIdAndUserIdOrderByCreatedAtDesc(Integer roomId, Integer userId);

    void deleteByRoomId(Integer roomId);
}
