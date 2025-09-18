package com.springleaf.knowseek.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 死信队列配置类
 * - 原始队列：file.processing.vectorize
 * - 死信交换机：file.processing.dlx
 * - 死信队列：file.processing.vectorize.failed
 */
@Configuration
public class RabbitMQDeadLetterConfig {

    // ========== 原始业务队列配置 ==========

    public static final String VECTORIZE_QUEUE = "file.processing.vectorize";
    public static final String VECTORIZE_EXCHANGE = "file.processing.exchange";
    public static final String VECTORIZE_ROUTING_KEY = "file.processing.vectorize";

    // ========== 死信队列配置 ==========

    public static final String DEAD_LETTER_EXCHANGE = "file.processing.dlx";
    public static final String DEAD_LETTER_QUEUE = "file.processing.vectorize.failed";
    public static final String DEAD_LETTER_ROUTING_KEY = "file.processing.vectorize.failed";

    @Bean
    public Queue vectorizeQueue() {
        return QueueBuilder.durable(VECTORIZE_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)           // 指定死信交换机
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)      // 死信路由键（可自定义）
                .build();
    }

    @Bean
    public DirectExchange vectorizeExchange() {
        return new DirectExchange(VECTORIZE_EXCHANGE);
    }

    @Bean
    public Binding vectorizeBinding() {
        return BindingBuilder.bind(vectorizeQueue())
                .to(vectorizeExchange())
                .with(VECTORIZE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }
}
