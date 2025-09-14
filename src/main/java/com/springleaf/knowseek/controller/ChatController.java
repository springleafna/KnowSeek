package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.AiMessageService;
import com.springleaf.knowseek.service.ChatService;
import com.springleaf.knowseek.service.SessionService;
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
    private final SessionService sessionService;
    private final AiMessageService aiMessageService;

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

    @DeleteMapping("/sessions/{sessionId}/messages")
    public Result<String> clearSessionMessages(@PathVariable Long sessionId) {
        try {
            aiMessageService.deleteMessagesBySessionId(sessionId);
            return Result.success("会话消息已清除");
        } catch (Exception e) {
            log.error("清除会话消息失败", e);
            return Result.error("清除会话消息失败：" + e.getMessage());
        }
    }
    
    // 会话管理相关接口 - 使用数据库存储
    @GetMapping("/sessions")
    public Result<List<SessionVO>> getUserSessions() {
        try {
            List<SessionVO> sessions = sessionService.getCurrentUserSessions();
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取用户会话列表失败", e);
            return Result.error("获取会话列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/sessions")
    public Result<SessionVO> createSession(@RequestBody @Valid SessionCreateDTO createDTO) {
        try {
            SessionVO session = sessionService.createSession(createDTO);
            return Result.success(session);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.error("创建会话失败：" + e.getMessage());
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<SessionVO> getSession(@PathVariable Long sessionId) {
        try {
            SessionVO session = sessionService.getSessionById(sessionId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return Result.error("获取会话信息失败：" + e.getMessage());
        }
    }

    @PutMapping("/sessions")
    public Result<Boolean> updateSession(@RequestBody @Valid SessionUpdateDTO updateDTO) {
        try {
            boolean success = sessionService.updateSession(updateDTO);
            return Result.success(success);
        } catch (Exception e) {
            log.error("更新会话失败", e);
            return Result.error("更新会话失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<String> deleteSession(@PathVariable Long sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            return Result.success("会话已删除");
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败：" + e.getMessage());
        }
    }
}