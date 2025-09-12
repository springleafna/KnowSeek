package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.AiChatRequestDTO;
import com.springleaf.knowseek.model.vo.AiChatResponseVO;
import com.springleaf.knowseek.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI对话接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 发送消息并获取AI回复
     */
    @PostMapping
    public Result<AiChatResponseVO> chat(@RequestBody @Valid AiChatRequestDTO requestDTO) {
        AiChatResponseVO responseVO = aiChatService.chat(requestDTO);
        return Result.success(responseVO);
    }
}