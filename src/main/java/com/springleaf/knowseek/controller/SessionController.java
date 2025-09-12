package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.SessionCreateDTO;
import com.springleaf.knowseek.model.dto.SessionUpdateDTO;
import com.springleaf.knowseek.model.vo.SessionVO;
import com.springleaf.knowseek.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会话管理接口
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    /**
     * 创建会话
     */
    @PostMapping
    public Result<SessionVO> createSession(@RequestBody @Valid SessionCreateDTO createDTO) {
        SessionVO sessionVO = sessionService.createSession(createDTO);
        return Result.success(sessionVO);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{id}")
    public Result<SessionVO> getSessionById(@PathVariable Long id) {
        SessionVO sessionVO = sessionService.getSessionById(id);
        return Result.success(sessionVO);
    }

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping
    public Result<List<SessionVO>> getCurrentUserSessions() {
        List<SessionVO> sessions = sessionService.getCurrentUserSessions();
        return Result.success(sessions);
    }

    /**
     * 更新会话
     */
    @PutMapping
    public Result<Boolean> updateSession(@RequestBody @Valid SessionUpdateDTO updateDTO) {
        boolean success = sessionService.updateSession(updateDTO);
        return Result.success(success);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSession(@PathVariable Long id) {
        boolean success = sessionService.deleteSession(id);
        return Result.success(success);
    }
}