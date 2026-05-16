package com.connecthub.message.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for message-service.
 *
 * Exchange topology:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Exchange: connecthub.messages  (topic)                            │
 * │                                                                     │
 * │  Routing keys published by message-service:                        │
 * │    message.sent        → notification-service consumes             │
 * │    message.sent        → websocket-service consumes (broadcast)    │
 * │    message.edited      → websocket-service consumes                │
 * │    message.deleted     → websocket-service consumes                │
 * │    message.status      → websocket-service consumes                │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Configuration
public class RabbitMQConfig {

    // ─── Exchange ──────────────────────────────────────────────────────────────
    public static final String MESSAGES_EXCHANGE = "connecthub.messages";

    // ─── Routing Keys (published by this service) ──────────────────────────────
    public static final String ROUTING_MESSAGE_SENT    = "message.sent";
    public static final String ROUTING_MESSAGE_EDITED  = "message.edited";
    public static final String ROUTING_MESSAGE_DELETED = "message.deleted";
    public static final String ROUTING_MESSAGE_STATUS  = "message.status";

    // ─── Queues (declared here so they exist before consumers start) ───────────
    // Notification queue
    public static final String QUEUE_NOTIFICATION_NEW_MESSAGE = "notification.new-message";

    // WebSocket broadcast queues
    public static final String QUEUE_WS_MESSAGE_SENT    = "ws.message.sent";
    public static final String QUEUE_WS_MESSAGE_EDITED  = "ws.message.edited";
    public static final String QUEUE_WS_MESSAGE_DELETED = "ws.message.deleted";
    public static final String QUEUE_WS_MESSAGE_STATUS  = "ws.message.status";

    // ─── Exchange Bean ─────────────────────────────────────────────────────────
    @Bean
    public TopicExchange messagesExchange() {
        return ExchangeBuilder.topicExchange(MESSAGES_EXCHANGE)
                .durable(true)
                .build();
    }

    // ─── Queue Beans ───────────────────────────────────────────────────────────

    @Bean
    public Queue notificationNewMessageQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_NEW_MESSAGE).build();
    }

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

    // ─── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding notificationNewMessageBinding(Queue notificationNewMessageQueue,
                                                  TopicExchange messagesExchange) {
        return BindingBuilder.bind(notificationNewMessageQueue)
                .to(messagesExchange)
                .with(ROUTING_MESSAGE_SENT);
    }

    @Bean
    public Binding wsMessageSentBinding(Queue wsMessageSentQueue,
                                         TopicExchange messagesExchange) {
        return BindingBuilder.bind(wsMessageSentQueue)
                .to(messagesExchange)
                .with(ROUTING_MESSAGE_SENT);
    }

    @Bean
    public Binding wsMessageEditedBinding(Queue wsMessageEditedQueue,
                                           TopicExchange messagesExchange) {
        return BindingBuilder.bind(wsMessageEditedQueue)
                .to(messagesExchange)
                .with(ROUTING_MESSAGE_EDITED);
    }

    @Bean
    public Binding wsMessageDeletedBinding(Queue wsMessageDeletedQueue,
                                            TopicExchange messagesExchange) {
        return BindingBuilder.bind(wsMessageDeletedQueue)
                .to(messagesExchange)
                .with(ROUTING_MESSAGE_DELETED);
    }

    @Bean
    public Binding wsMessageStatusBinding(Queue wsMessageStatusQueue,
                                           TopicExchange messagesExchange) {
        return BindingBuilder.bind(wsMessageStatusQueue)
                .to(messagesExchange)
                .with(ROUTING_MESSAGE_STATUS);
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
