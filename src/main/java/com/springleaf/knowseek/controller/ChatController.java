package com.springleaf.knowseek.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.ChatRequestDTO;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.vo.ChatResponseVO;
import com.springleaf.knowseek.model.vo.MessageVO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.ChatService;
import com.springleaf.knowseek.service.MessageService;
import com.springleaf.knowseek.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MessageService messageService;

    /**
     * 普通AI对话
     */
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

    /**
     * AI流式对话
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody @Valid ChatRequestDTO requestDTO) {
        return chatService.streamChat(requestDTO);
    }

    /**
     * 获取用户会话列表
     */
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

    /**
     * 获取对话信息
     */
    @GetMapping("/getSession/{sessionId}")
    public Result<SessionVO> getSession(@PathVariable Long sessionId) {
        try {
            SessionVO session = sessionService.getSessionById(sessionId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取会话信息失败", e);
            return Result.error("获取会话信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/createSession")
    public Result<SessionVO> createSession(@RequestBody @Valid SessionCreateDTO createDTO) {
        try {
            SessionVO session = sessionService.createSession(createDTO);
            return Result.success(session);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return Result.error("创建会话失败：" + e.getMessage());
        }
    }

    @PutMapping("/updateSession")
    public Result<Boolean> updateSession(@RequestBody @Valid SessionUpdateDTO updateDTO) {
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            boolean success = sessionService.updateSession(updateDTO, currentUserId);
            return Result.success(success);
        } catch (Exception e) {
            log.error("更新会话失败", e);
            return Result.error("更新会话失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/deleteSession/{sessionId}")
    public Result<String> deleteSession(@PathVariable Long sessionId) {
        try {
            sessionService.deleteSession(sessionId);
            return Result.success("会话已删除");
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话历史记录
     */
    @GetMapping("/messages/{sessionId}")
    public Result<List<MessageVO>> getSessionMessages(@PathVariable Long sessionId) {
        try {
            List<MessageVO> messages = messageService.getMessagesBySessionId(sessionId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取会话消息记录失败", e);
            return Result.error("获取消息记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除会话历史记录
     */
    @DeleteMapping("/deleteMessages/{sessionId}")
    public Result<Void> deleteSessionMessages(@PathVariable Long sessionId) {
        sessionService.deleteMessages(sessionId);
        return Result.success();
    }
}