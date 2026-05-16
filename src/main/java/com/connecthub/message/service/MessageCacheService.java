package com.connecthub.message.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Redis cache layer for Message Service.
 *
 * What we cache:
 *   1. Recent messages per room (last 20) — key: msg:room:{roomId}:recent
 *      TTL: 10 minutes. Invalidated on new message / edit / delete.
 *
 *   2. Unread message count per user per room — key: msg:unread:{userId}:{roomId}
 *      TTL: 5 minutes. Invalidated on read receipt.
 *
 * This avoids repeated MySQL queries for the most common chat operations:
 *   - Opening a room → loads recent messages from Redis (cache hit)
 *   - Badge count on room list → loads from Redis (cache hit)
 */
@Service
public class MessageCacheService {

    private static final Logger log = Logger.getLogger(MessageCacheService.class.getName());

    private static final String KEY_RECENT  = "msg:room:%d:recent";
    private static final String KEY_UNREAD  = "msg:unread:%d:%d";   // userId, roomId

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${message.redis.recent.ttl.seconds:600}")
    private long recentTtl;

    @Value("${message.redis.unread.ttl.seconds:300}")
    private long unreadTtl;

    public MessageCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─── Recent messages ──────────────────────────────────────────────────────

    /** Store a list of recent messages for a room. Called after MySQL fetch. */
    public void cacheRecentMessages(Integer roomId, List<?> messages) {
        String key = String.format(KEY_RECENT, roomId);
        redisTemplate.opsForValue().set(key, messages, recentTtl, TimeUnit.SECONDS);
        log.fine("Cached " + messages.size() + " recent messages for roomId=" + roomId);
    }

    /** Get cached recent messages. Returns null on cache miss. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentMessages(Integer roomId) {
        String key = String.format(KEY_RECENT, roomId);
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof List<?> list) {
            log.fine("Cache HIT recent messages roomId=" + roomId);
            return (List<Map<String, Object>>) list;
        }
        log.fine("Cache MISS recent messages roomId=" + roomId);
        return null;
    }

    /** Invalidate recent message cache when a new message is sent or deleted. */
    public void invalidateRecentMessages(Integer roomId) {
        String key = String.format(KEY_RECENT, roomId);
        redisTemplate.delete(key);
        log.fine("Invalidated recent message cache for roomId=" + roomId);
    }

    // ─── Unread count ─────────────────────────────────────────────────────────

    /** Increment unread count for a user in a room (called on new message). */
    public void incrementUnread(Integer userId, Integer roomId) {
        String key = String.format(KEY_UNREAD, userId, roomId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, unreadTtl, TimeUnit.SECONDS);
    }

    /** Reset unread count to 0 (called on read receipt). */
    public void clearUnread(Integer userId, Integer roomId) {
        String key = String.format(KEY_UNREAD, userId, roomId);
        redisTemplate.delete(key);
    }

    /** Get cached unread count. Returns null on cache miss. */
    public Long getUnreadCount(Integer userId, Integer roomId) {
        String key = String.format(KEY_UNREAD, userId, roomId);
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}