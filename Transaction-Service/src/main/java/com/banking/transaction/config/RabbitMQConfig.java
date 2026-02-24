package com.banking.transaction.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queues
    @Bean
    public Queue transferCompletedQueue() {
        return new Queue("transfer-completed-queue", true);
    }

    // Topic Exchange
    @Bean
    public TopicExchange transferExchange() {
        return new TopicExchange("transfers-exchange", true, false);
    }

    // Bindings
    @Bean
    public Binding transferBinding(Queue transferCompletedQueue, TopicExchange transferExchange) {
        return BindingBuilder.bind(transferCompletedQueue)
                .to(transferExchange)
                .with("transfer.#");
    }
}
