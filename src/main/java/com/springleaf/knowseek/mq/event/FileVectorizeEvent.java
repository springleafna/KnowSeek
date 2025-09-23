package com.springleaf.knowseek.mq.event;

import com.springleaf.knowseek.model.bo.VectorBO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * 文件向量化处理执行事件
 */
@Component
public class FileVectorizeEvent extends BaseEvent<FileVectorizeEvent.FileVectorizeMessage> {

    @Value("${spring.rabbitmq.topic.file-processing-vectorize}")
    private String topic;

    @Override
    public EventMessage<FileVectorizeMessage> buildEventMessage(FileVectorizeMessage data) {
        return EventMessage.<FileVectorizeMessage>builder()
                .id(UUID.randomUUID().toString())
                .timestamp(new Date())
                .data(data)
                .build();
    }

    @Override
    public String topic() {
        return topic;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileVectorizeMessage {

        private VectorBO vectorBO;

        /**
         * 文件地址
         */
        private String location;

        /**
         * 文件名
         */
        private String fileName;

        /**
         * 文件类型
         */
        private String extension;

    }

}
