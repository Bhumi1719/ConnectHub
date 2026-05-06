package com.connecthub.room.repository;

import com.connecthub.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    void deleteByRoomId(Integer roomId);

}
