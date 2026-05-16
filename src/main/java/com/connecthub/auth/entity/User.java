package com.connecthub.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "username")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 100)
    private String fullName;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 200)
    private String bio;

    // ONLINE, AWAY, DND, INVISIBLE
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ONLINE";

    // LOCAL, GOOGLE, GITHUB
    @Column(length = 20)
    @Builder.Default
    private String provider = "LOCAL";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime lastSeenAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
