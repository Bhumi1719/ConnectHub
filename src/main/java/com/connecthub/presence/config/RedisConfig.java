package com.connecthub.presence.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
 
/**
 * Redis configuration for Presence Service.
 *
 * Redis is used to cache user presence (online/offline status) so that
 * every ping does NOT hit MySQL. Flow:
 *   setOnline  → write to Redis (TTL 90s) + write to MySQL
 *   setOffline → delete from Redis + update MySQL
 *   pingSession→ refresh TTL in Redis only  (MySQL skipped)
 *   getPresence→ Redis first, MySQL fallback
 *
 * Key pattern:  presence:userId:{userId}   → JSON of status, sessionId, etc.
 *               presence:session:{sessionId} → userId (for disconnect lookup)
 */
@Configuration
public class RedisConfig {
 
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
 
        // Keys as plain strings
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
 
        // Values as JSON — human-readable in Redis CLI
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
 
        template.afterPropertiesSet();
        return template;
    }
}