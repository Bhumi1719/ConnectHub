package com.connecthub.websocket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for websocket-service.
 *
 * This service is both:
 *   CONSUMER — receives message events from message-service and presence
 *              events from presence-service, then broadcasts via STOMP.
 *   PUBLISHER — publishes presence events when users connect/disconnect.
 *
 * Queues consumed:
 *   ws.message.sent     ← message-service publishes when a message is sent
 *   ws.message.edited   ← message-service publishes when a message is edited
 *   ws.message.deleted  ← message-service publishes when a message is deleted
 *   ws.message.status   ← message-service publishes on delivery status change
 *   ws.presence.update  ← presence-service publishes on online/offline change
 *
 * Exchange published to:
 *   connecthub.presence  (topic) — presence events this service originates
 */
@Configuration
public class RabbitMQConfig {

    // ─── Exchanges ─────────────────────────────────────────────────────────────
    public static final String MESSAGES_EXCHANGE = "connecthub.messages";
    public static final String PRESENCE_EXCHANGE  = "connecthub.presence";

    // ─── Routing keys this service publishes ───────────────────────────────────
    public static final String ROUTING_PRESENCE_ONLINE  = "presence.online";
    public static final String ROUTING_PRESENCE_OFFLINE = "presence.offline";
    public static final String ROUTING_PRESENCE_UPDATE  = "presence.update";

    // ─── Queues consumed by this service ──────────────────────────────────────
    public static final String QUEUE_WS_MESSAGE_SENT    = "ws.message.sent";
    public static final String QUEUE_WS_MESSAGE_EDITED  = "ws.message.edited";
    public static final String QUEUE_WS_MESSAGE_DELETED = "ws.message.deleted";
    public static final String QUEUE_WS_MESSAGE_STATUS  = "ws.message.status";
    public static final String QUEUE_WS_PRESENCE_UPDATE = "ws.presence.update";

    // ─── Exchange Beans ────────────────────────────────────────────────────────
    @Bean
    public TopicExchange messagesExchange() {
        return ExchangeBuilder.topicExchange(MESSAGES_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange presenceExchange() {
        return ExchangeBuilder.topicExchange(PRESENCE_EXCHANGE).durable(true).build();
    }

    // ─── Queue Beans (declared here — shared across services) ─────────────────

    @Bean
    public Queue wsMessageSentQueue() {
        return QueueBuilder.durable(QUEUE_WS_MESSAGE_SENT).build();
    }

    @Bean
    public Queue wsMessageEditedQueue() {
        return QueueBuilder.durable(QUEUE_WS_MESSAGE_EDITED).build();
    }

    @Bean
    public Queue wsMessageDeletedQueue() {
        return QueueBuilder.durable(QUEUE_WS_MESSAGE_DELETED).build();
    }

    @Bean
    public Queue wsMessageStatusQueue() {
        return QueueBuilder.durable(QUEUE_WS_MESSAGE_STATUS).build();
    }

    @Bean
    public Queue wsPresenceUpdateQueue() {
        return QueueBuilder.durable(QUEUE_WS_PRESENCE_UPDATE).build();
    }

    // ─── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding wsPresenceUpdateBinding(Queue wsPresenceUpdateQueue,
                                            TopicExchange presenceExchange) {
        // All presence.* routing keys from presence-service
        return BindingBuilder.bind(wsPresenceUpdateQueue)
                .to(presenceExchange)
                .with("presence.*");
    }

    // ─── JSON Converter ────────────────────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
