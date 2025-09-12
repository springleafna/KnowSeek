package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.AiMessageCreateDTO;
import com.springleaf.knowseek.model.vo.AiMessageVO;
import com.springleaf.knowseek.service.AiMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI消息管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class AiMessageController {

    private final AiMessageService aiMessageService;

    /**
     * 创建消息
     */
    @PostMapping
    public Result<AiMessageVO> createMessage(@RequestBody @Valid AiMessageCreateDTO createDTO) {
        AiMessageVO messageVO = aiMessageService.createMessage(createDTO);
        return Result.success(messageVO);
    }

    /**
     * 获取消息详情
     */
    @GetMapping("/{messageId}")
    public Result<AiMessageVO> getMessageById(@PathVariable Long messageId) {
        AiMessageVO messageVO = aiMessageService.getMessageById(messageId);
        return Result.success(messageVO);
    }

    /**
     * 获取会话的消息列表
     */
    @GetMapping("/session/{sessionId}")
    public Result<List<AiMessageVO>> getMessagesBySessionId(@PathVariable Long sessionId) {
        List<AiMessageVO> messages = aiMessageService.getMessagesBySessionId(sessionId);
        return Result.success(messages);
    }

    /**
     * 删除会话的所有消息
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Boolean> deleteMessagesBySessionId(@PathVariable Long sessionId) {
        boolean success = aiMessageService.deleteMessagesBySessionId(sessionId);
        return Result.success(success);
    }
}