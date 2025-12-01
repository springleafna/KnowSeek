package com.springleaf.knowseek.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQDeadLetterConfig {

    @Value("${spring.rabbitmq.custom.vectorize.queue}")
    private String vectorizeQueueName;
    @Value("${spring.rabbitmq.custom.vectorize.exchange}")
    private String vectorizeExchangeName;
    @Value("${spring.rabbitmq.custom.vectorize.routing-key}")
    private String vectorizeRoutingKey;
    @Value("${spring.rabbitmq.custom.dead-letter.exchange}")
    private String deadLetterExchangeName;
    @Value("${spring.rabbitmq.custom.dead-letter.queue}")
    private String deadLetterQueueName;
    @Value("${spring.rabbitmq.custom.dead-letter.routing-key}")
    private String deadLetterRoutingKey;

    @Bean
    public Queue vectorizeQueue() {
        return QueueBuilder.durable(vectorizeQueueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }

    @Bean
    public DirectExchange vectorizeExchange() {
        return new DirectExchange(vectorizeExchangeName);
    }

    @Bean
    public Binding vectorizeBinding() {
        return BindingBuilder.bind(vectorizeQueue())
                .to(vectorizeExchange())
                .with(vectorizeRoutingKey);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchangeName);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(deadLetterRoutingKey);
    }
}
