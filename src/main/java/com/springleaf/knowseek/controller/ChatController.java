package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.CreateConversationDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.ConversationVO;
import com.springleaf.knowseek.service.ChatService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@Validated
@SaCheckLogin
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public Result<ChatResponseVO> chat(@RequestBody @Valid ChatRequestDTO requestDTO) {
        try {
            ChatResponseVO response = chatService.chat(requestDTO);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI对话失败：" + e.getMessage());
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody @Valid ChatRequestDTO requestDTO) {
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
    
    // 会话管理相关接口
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getUserConversations() {
        try {
            List<ConversationVO> conversations = chatService.getUserConversations();
            return Result.success(conversations);
        } catch (Exception e) {
            log.error("获取用户会话列表失败", e);
            return Result.error("获取会话列表失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/conversations")
    public Result<ConversationVO> createConversation(@RequestBody @Valid CreateConversationDTO createDTO) {
        try {
            ConversationVO conversation = chatService.createConversation(createDTO);
            return Result.success(conversation);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.error("创建会话失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationVO> getConversation(@PathVariable String conversationId) {
        try {
            ConversationVO conversation = chatService.getConversation(conversationId);
            if (conversation == null) {
                return Result.error("会话不存在");
            }
            return Result.success(conversation);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return Result.error("获取会话信息失败：" + e.getMessage());
        }
    }
    
    @DeleteMapping("/conversations/{conversationId}")
    public Result<String> deleteConversation(@PathVariable String conversationId) {
        try {
            chatService.deleteConversation(conversationId);
            return Result.success("会话已删除");
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败：" + e.getMessage());
        }
    }
}