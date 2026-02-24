package com.banking.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue transferCompletedQueue() {
        return new Queue("transfer-completed-queue", true);
    }
}
