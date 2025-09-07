package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.KnowledgeBaseCreateDTO;
import com.springleaf.knowseek.model.dto.KnowledgeBaseUpdateDTO;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.KnowledgeBaseVO;
import com.springleaf.knowseek.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledgeBase")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 获取文件列表
     */
    @GetMapping("/getFileList")
    public Result<List<FileItemVO>> getFileList() {
        return Result.success(knowledgeBaseService.getFileList());
    }

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Result<Void> createKnowledgeBase(@RequestBody @Validated KnowledgeBaseCreateDTO createDTO) {
        knowledgeBaseService.createKnowledgeBase(createDTO);
        return Result.success();
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success();
    }

    /**
     * 获取当前用户的知识库列表
     */
    @GetMapping("/list")
    public Result<List<KnowledgeBaseVO>> listMyKnowledgeBases() {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listKnowledgeBasesByCurrentUser();
        return Result.success(list);
    }

    /**
     * 更新知识库名称
     */
    @PutMapping("/update")
    public Result<Void> updateKnowledgeBaseName(@RequestBody @Validated KnowledgeBaseUpdateDTO updateDTO) {
        knowledgeBaseService.updateKnowledgeBaseName(updateDTO);
        return Result.success();
    }

    /**
     * 获取单个知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getKnowledgeBaseById(@PathVariable Long id) {
        KnowledgeBaseVO vo = knowledgeBaseService.getKnowledgeBaseById(id);
        if (vo == null) {
            return Result.error("知识库不存在");
        }
        return Result.success(vo);
    }
}
