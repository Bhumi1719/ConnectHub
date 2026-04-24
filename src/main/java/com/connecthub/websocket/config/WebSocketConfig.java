package com.connecthub.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker prefix — clients subscribe to these
        // /topic/room/{roomId}  → group room messages
        // /topic/user/{userId}  → personal notifications
        // /topic/presence       → online/offline updates
        registry.enableSimpleBroker("/topic", "/queue");

        // App prefix — client sends messages here
        // /app/chat.send    → send message
        // /app/chat.typing  → typing indicator
        // /app/chat.read    → read receipt
        registry.setApplicationDestinationPrefixes("/app");

        // User specific prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint — clients connect here
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS fallback for browsers behind proxies
    }
}
