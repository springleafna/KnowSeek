package com.springleaf.knowseek.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queues = "${spring.rabbitmq.custom.dead-letter.queue}")
    public void handleFailedMessage(String message,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.error("接收到死信消息: {}, deliveryTag: {}", message, deliveryTag);

        try {
            processFailedMessage(message);
            log.info("死信消息处理完成: {}", message);
        } catch (Exception e) {
            log.error("处理死信消息时发生异常: {}", message, e);
        }
    }

    private void processFailedMessage(String message) {
        log.warn("记录失败消息到系统: {}", message);
    }
}