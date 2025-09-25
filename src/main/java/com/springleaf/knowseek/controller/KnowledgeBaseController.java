package com.springleaf.knowseek.controller;

import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.KnowledgeBaseCreateDTO;
import com.springleaf.knowseek.model.dto.KnowledgeBaseUpdateDTO;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.KnowledgeBaseVO;
import com.springleaf.knowseek.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
     * 获取指定知识库的文件列表
     */
    @GetMapping("/getFileList/{id}")
    public Result<List<FileItemVO>> getFileList(@PathVariable @NotNull(message = "知识库ID不能为空！") Long id) {
        return Result.success(knowledgeBaseService.getFileList(id));
    }

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Result<Void> createKnowledgeBase(@RequestBody @Valid KnowledgeBaseCreateDTO createDTO) {
        knowledgeBaseService.createKnowledgeBase(createDTO);
        return Result.success();
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable @NotNull(message = "知识库ID不能为空！") Long id) {
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
     * 编辑知识库
     */
    @PutMapping("/update")
    public Result<Void> updateKnowledgeBaseName(@RequestBody @Valid KnowledgeBaseUpdateDTO updateDTO) {
        knowledgeBaseService.updateKnowledgeBaseName(updateDTO);
        return Result.success();
    }

    /**
     * 获取单个知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getKnowledgeBaseById(@PathVariable @NotNull(message = "知识库ID不能为空！") Long id) {
        KnowledgeBaseVO vo = knowledgeBaseService.getKnowledgeBaseById(id);
        if (vo == null) {
            return Result.error("知识库不存在");
        }
        return Result.success(vo);
    }

    /**
     * 选中某个知识库设为用户主知识库
     */
    @PostMapping("/setPrimary")
    public Result<Void> setPrimary(@RequestParam("id") @NotNull(message = "知识库ID不能为空！") Long id) {
        knowledgeBaseService.setPrimaryKnowledgeBase(id);
        return Result.success();
    }
}
