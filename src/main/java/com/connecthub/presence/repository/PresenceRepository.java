package com.connecthub.presence.repository;

import com.connecthub.presence.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresenceRepository extends JpaRepository<UserPresence, Integer> {

    Optional<UserPresence> findByUserId(Integer userId);

    Optional<UserPresence> findBySessionId(String sessionId);

    List<UserPresence> findByStatus(String status);

    // Bulk presence — get presence for multiple users at once
    @Query("SELECT p FROM UserPresence p WHERE p.userId IN :userIds")
    List<UserPresence> findByUserIdIn(@Param("userIds") List<Integer> userIds);

    // All online users
    @Query("SELECT p FROM UserPresence p WHERE p.status = 'ONLINE'")
    List<UserPresence> findOnlineUsers();

    // Stale sessions — lastPingAt older than threshold
    @Query("SELECT p FROM UserPresence p WHERE p.lastPingAt < :threshold")
    List<UserPresence> findStaleSessions(@Param("threshold") LocalDateTime threshold);

    void deleteByUserId(Integer userId);

    void deleteBySessionId(String sessionId);
}
