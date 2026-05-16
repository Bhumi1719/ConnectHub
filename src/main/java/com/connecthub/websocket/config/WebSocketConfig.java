package com.connecthub.websocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + Redis configuration.
 *
 * Redis Pub/Sub channels used:
 *   ws:chat:{roomId}   — chat messages for a room (broadcast across instances)
 *   ws:presence        — global presence updates
 *   ws:typing:{roomId} — typing indicators
 *
 * Why Redis pub/sub here?
 *   If you run 2+ instances of websocket-service, a message sent to instance-1
 *   must reach users connected to instance-2. Redis pub/sub solves this.
 *   Each instance subscribes to the relevant channels and forwards to its
 *   local STOMP /topic/* subscribers.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // ─── Redis pub/sub channel names ──────────────────────────────────────────
    public static final String REDIS_CHANNEL_PRESENCE = "ws:presence";
    public static final String REDIS_CHANNEL_CHAT_PREFIX = "ws:chat:";

    @Value("${cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker prefix — clients subscribe to these
        // /topic/room/{roomId}  → group room messages
        // /topic/user/{userId}  → personal notifications
        // /topic/presence       → online/offline updates
        registry.enableSimpleBroker("/topic", "/queue");

        // App prefix — client sends messages here
        registry.setApplicationDestinationPrefixes("/app");

        // User specific prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint — used by the Angular client (brokerURL: ws://...)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);

        // SockJS fallback endpoint — kept for browser compatibility if needed
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    // ─── Redis Template ───────────────────────────────────────────────────────

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }

    // ─── Redis Pub/Sub: listener container ───────────────────────────────────
    // This container receives messages published to Redis channels and routes
    // them to the appropriate MessageListener.

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory factory,
            RedisPresenceSubscriber presenceSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // Subscribe to global presence channel
        MessageListenerAdapter presenceAdapter = new MessageListenerAdapter(presenceSubscriber, "onMessage");
        presenceAdapter.setSerializer(new GenericJackson2JsonRedisSerializer());
        container.addMessageListener(presenceAdapter, new ChannelTopic(REDIS_CHANNEL_PRESENCE));

        return container;
    }

    // ─── Presence channel topic bean (used by publisher) ─────────────────────

    @Bean
    public ChannelTopic presenceTopic() {
        return new ChannelTopic(REDIS_CHANNEL_PRESENCE);
    }
}