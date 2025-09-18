package com.springleaf.knowseek.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者 - 处理消费失败的消息
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    @RabbitListener(queuesToDeclare = @Queue(value = "file.processing.vectorize.failed"))
    public void handleFailedMessage(String message,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.error("接收到死信消息: {}, deliveryTag: {}", message, deliveryTag);

        try {
            // 在这里处理失败的消息，可以：
            // 1. 记录到失败日志表
            // 2. 发送告警通知
            // 3. 人工介入处理
            // 4. 或者根据业务需要进行其他处理

            processFailedMessage(message);

            log.info("死信消息处理完成: {}", message);

        } catch (Exception e) {
            log.error("处理死信消息时发生异常: {}", message, e);
            // 这里可以考虑将消息存储到持久化存储中，供后续人工处理
        }
    }

    /**
     * 处理失败的消息
     */
    private void processFailedMessage(String message) {
        // 解析消息内容
        // 可以尝试识别失败原因
        // 记录详细的失败信息
        // 如果是临时性错误，可以考虑重新发送到原队列

        log.warn("记录失败消息到系统: {}", message);

        // TODO: 实现具体的业务处理逻辑
        // 例如：
        // 1. 更新文件处理状态为失败
        // 2. 发送邮件或短信通知管理员
        // 3. 记录到专门的失败处理表
    }
}