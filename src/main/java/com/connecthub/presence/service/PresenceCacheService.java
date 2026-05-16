package com.connecthub.presence.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Redis cache layer for user presence.
 *
 * Keys used:
 *   presence:userId:{userId}      → Map of status, sessionId, deviceType, lastPingAt
 *   presence:session:{sessionId}  → userId string  (for fast disconnect lookup)
 */
@Service
public class PresenceCacheService {

    private static final Logger log = Logger.getLogger(PresenceCacheService.class.getName());

    private static final String KEY_USER   = "presence:userId:";
    private static final String KEY_SESSION = "presence:session:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${presence.redis.ttl.seconds:90}")
    private long ttlSeconds;

    public PresenceCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─── Set user ONLINE in Redis ─────────────────────────────────────────────

    public void setOnline(Integer userId, String sessionId, String deviceType) {
        String userKey    = KEY_USER + userId;
        String sessionKey = KEY_SESSION + sessionId;

        Map<String, Object> data = new HashMap<>();
        data.put("userId",     userId);
        data.put("status",     "ONLINE");
        data.put("sessionId",  sessionId);
        data.put("deviceType", deviceType != null ? deviceType : "WEB");
        data.put("lastPingAt", LocalDateTime.now().toString());

        redisTemplate.opsForValue().set(userKey, data, ttlSeconds, TimeUnit.SECONDS);

        // Reverse index: sessionId → userId (used during disconnect)
        redisTemplate.opsForValue().set(sessionKey, userId.toString(), ttlSeconds, TimeUnit.SECONDS);

        log.fine("Redis: set ONLINE userId=" + userId + " ttl=" + ttlSeconds + "s");
    }

    // ─── Set user OFFLINE in Redis ────────────────────────────────────────────

    public void setOffline(Integer userId) {
        String userKey = KEY_USER + userId;

        // Read existing entry to delete session reverse-index too
        Object existing = redisTemplate.opsForValue().get(userKey);
        if (existing instanceof Map<?,?> map) {
            Object sessionId = map.get("sessionId");
            if (sessionId != null) {
                redisTemplate.delete(KEY_SESSION + sessionId);
            }
        }

        redisTemplate.delete(userKey);
        log.fine("Redis: deleted presence for userId=" + userId);
    }

    // ─── Ping — just refresh TTL, skip MySQL ─────────────────────────────────

    public void refreshSession(String sessionId) {
        String sessionKey = KEY_SESSION + sessionId;
        String userIdStr  = (String) redisTemplate.opsForValue().get(sessionKey);

        if (userIdStr != null) {
            String userKey = KEY_USER + userIdStr;
            Object data = redisTemplate.opsForValue().get(userKey);
            if (data instanceof Map<?,?> map) {
                Map<String, Object> updated = new HashMap<>((Map<String, Object>) map);
                updated.put("lastPingAt", LocalDateTime.now().toString());
                redisTemplate.opsForValue().set(userKey, updated, ttlSeconds, TimeUnit.SECONDS);
            }
            redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
            log.fine("Redis: refreshed session=" + sessionId);
        }
    }

    // ─── Get cached status ────────────────────────────────────────────────────

    public String getStatus(Integer userId) {
        Object data = redisTemplate.opsForValue().get(KEY_USER + userId);
        if (data instanceof Map<?,?> map) {
            Object status = map.get("status");
            return status != null ? status.toString() : null;
        }
        return null;
    }

    // ─── Check online ─────────────────────────────────────────────────────────

    public boolean isOnline(Integer userId) {
        return redisTemplate.hasKey(KEY_USER + userId);
    }

    // ─── Get userId from sessionId (for disconnect) ───────────────────────────

    public Integer getUserIdBySession(String sessionId) {
        Object val = redisTemplate.opsForValue().get(KEY_SESSION + sessionId);
        if (val != null) {
            try { return Integer.parseInt(val.toString()); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // ─── Update status only (AWAY, DND, etc.) ────────────────────────────────

    public void updateStatus(Integer userId, String newStatus) {
        String userKey = KEY_USER + userId;
        Object data = redisTemplate.opsForValue().get(userKey);
        if (data instanceof Map<?,?> map) {
            Map<String, Object> updated = new HashMap<>((Map<String, Object>) map);
            updated.put("status", newStatus);
            // Preserve remaining TTL
            Long ttl = redisTemplate.getExpire(userKey, TimeUnit.SECONDS);
            long remaining = (ttl != null && ttl > 0) ? ttl : ttlSeconds;
            redisTemplate.opsForValue().set(userKey, updated, remaining, TimeUnit.SECONDS);
        }
    }
}