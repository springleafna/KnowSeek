package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@Validated
@SaCheckLogin
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public Result<ChatResponseVO> chat(@Valid @RequestBody ChatRequestDTO requestDTO) {
        try {
            ChatResponseVO response = chatService.chat(requestDTO);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI对话失败：" + e.getMessage());
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequestDTO requestDTO) {
        return chatService.streamChat(requestDTO);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Result<String> clearConversation(@PathVariable String conversationId) {
        try {
            chatService.clearConversation(conversationId);
            return Result.success("对话历史已清除");
        } catch (Exception e) {
            log.error("清除对话历史失败", e);
            return Result.error("清除对话历史失败：" + e.getMessage());
        }
    }
}