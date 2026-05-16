package com.connecthub.auth.repository;

import com.connecthub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByStatus(String status);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.username ASC")
    List<User> findActiveUsers();

    // Search users by username (for adding to groups / starting DM)
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% AND u.isActive = true")
    List<User> searchByUsername(@Param("keyword") String keyword);

    void deleteByUserId(Integer userId);
}
