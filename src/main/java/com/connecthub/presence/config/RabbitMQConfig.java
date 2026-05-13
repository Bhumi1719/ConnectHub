package com.connecthub.presence.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for presence-service.
 *
 * This service CONSUMES presence events from websocket-service:
 *   - presence.online   → set user ONLINE in DB
 *   - presence.offline  → set user INVISIBLE in DB
 *   - presence.update   → update user status in DB
 *
 * Queues:
 *   presence.db.online   → bound to connecthub.presence / presence.online
 *   presence.db.offline  → bound to connecthub.presence / presence.offline
 *   presence.db.update   → bound to connecthub.presence / presence.update
 */
@Configuration
public class RabbitMQConfig {

    // Exchange (shared — declared by websocket-service, redeclared here for safety)
    public static final String PRESENCE_EXCHANGE = "connecthub.presence";

    // Queues owned by this service
    public static final String QUEUE_PRESENCE_DB_ONLINE  = "presence.db.online";
    public static final String QUEUE_PRESENCE_DB_OFFLINE = "presence.db.offline";
    public static final String QUEUE_PRESENCE_DB_UPDATE  = "presence.db.update";

    // ─── Exchange ──────────────────────────────────────────────────────────────
    @Bean
    public TopicExchange presenceExchange() {
        return ExchangeBuilder.topicExchange(PRESENCE_EXCHANGE).durable(true).build();
    }

    // ─── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue presenceDbOnlineQueue() {
        return QueueBuilder.durable(QUEUE_PRESENCE_DB_ONLINE).build();
    }

    @Bean
    public Queue presenceDbOfflineQueue() {
        return QueueBuilder.durable(QUEUE_PRESENCE_DB_OFFLINE).build();
    }

    @Bean
    public Queue presenceDbUpdateQueue() {
        return QueueBuilder.durable(QUEUE_PRESENCE_DB_UPDATE).build();
    }

    // ─── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding presenceOnlineBinding(Queue presenceDbOnlineQueue,
                                          TopicExchange presenceExchange) {
        return BindingBuilder.bind(presenceDbOnlineQueue)
                .to(presenceExchange)
                .with("presence.online");
    }

    @Bean
    public Binding presenceOfflineBinding(Queue presenceDbOfflineQueue,
                                           TopicExchange presenceExchange) {
        return BindingBuilder.bind(presenceDbOfflineQueue)
                .to(presenceExchange)
                .with("presence.offline");
    }

    @Bean
    public Binding presenceUpdateBinding(Queue presenceDbUpdateQueue,
                                          TopicExchange presenceExchange) {
        return BindingBuilder.bind(presenceDbUpdateQueue)
                .to(presenceExchange)
                .with("presence.update");
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
