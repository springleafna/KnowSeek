package com.springleaf.knowseek.controller;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.*;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.UploadCompleteVO;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.model.vo.UploadProgressVO;
import com.springleaf.knowseek.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 获取当前用户的文件列表
     */
    @GetMapping("/getFileList")
    public Result<PageInfo<FileItemVO>> getFileList(FilePageDTO filePageDTO) {
        PageInfo<FileItemVO> pageInfo = fileService.getFileList(filePageDTO);
        return Result.success(pageInfo);
    }

    /**
     * 文件上传初始化
     */
    @PostMapping("/init")
    public Result<UploadInitVO> initiateMultipartUpload(@RequestBody @Valid FileUploadChunkInitDTO fileUploadChunkInitDTO) {
        UploadInitVO uploadInitVO = fileService.initFileUpload(fileUploadChunkInitDTO);
        return Result.success(uploadInitVO);
    }

    /**
     * 获取上传进度
     */
    @GetMapping("/progress")
    public Result<UploadProgressVO> getUploadProgress(String uploadId, String fileKey) {
        return Result.success(fileService.getUploadProgress(uploadId, fileKey));
    }

    /**
     * 保存分片信息
     */
    @PostMapping("/chunk")
    public Result<Void> uploadChunk(@RequestBody @Valid FileUploadChunkDTO fileUploadChunkDTO) {
        fileService.uploadChunk(fileUploadChunkDTO);
        return Result.success();
    }

    /**
     * 完成分片上传，进行合并
     */
    @PostMapping("/complete")
    public Result<UploadCompleteVO> completeChunkUpload(@RequestBody FileUploadCompleteDTO fileUploadCompleteDTO) {
        UploadCompleteVO uploadCompleteVO = fileService.completeChunkUpload(fileUploadCompleteDTO);
        return Result.success(uploadCompleteVO);
    }

    /**
     * 暂停上传
     */
    @PostMapping("/pause")
    public Result<Void> pauseUpload(@RequestBody @Valid FileUploadPauseDTO fileUploadPauseDTO) {
        fileService.pauseUpload(fileUploadPauseDTO);
        return Result.success();
    }

    /**
     * 恢复上传
     */
    @PostMapping("/resume")
    public Result<UploadInitVO> resumeUpload(@RequestBody @Valid FileUploadPauseDTO fileUploadPauseDTO) {
        UploadInitVO uploadInitVO = fileService.resumeUpload(fileUploadPauseDTO);
        return Result.success(uploadInitVO);
    }

    /**
     * 取消上传
     */
    @PostMapping("/cancel")
    public Result<Void> cancelUpload(@RequestBody FileUploadCancelDTO fileUploadCancelDTO) {
        fileService.cancelUpload(fileUploadCancelDTO);
        return Result.success();
    }
}