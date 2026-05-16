package com.connecthub.notification.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification-service.
 *
 * This service is a CONSUMER only.
 * Queue names must match what message-service declared.
 *
 * Queue consumed: notification.new-message
 *   → bound to exchange:  connecthub.messages
 *   → routing key:        message.sent
 */
@Configuration
public class RabbitMQConfig {

    // Queue consumed by this service (declared by message-service)
    public static final String QUEUE_NOTIFICATION_NEW_MESSAGE = "notification.new-message";

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
