package com.springleaf.knowseek.controller;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.log.OperationLogRecord;
import com.springleaf.knowseek.log.OperationType;
import com.springleaf.knowseek.model.dto.*;
import com.springleaf.knowseek.model.vo.*;
import com.springleaf.knowseek.service.FileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 获取当前用户的文件列表
     */
    @GetMapping("/getFileList")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.QUERY, description = "获取文件列表")
    public Result<PageInfo<FileItemVO>> getFileList(FilePageDTO filePageDTO) {
        PageInfo<FileItemVO> pageInfo = fileService.getFileList(filePageDTO);
        return Result.success(pageInfo);
    }

    /**
     * 获取文件的所有分片文本内容
     */
    @GetMapping("/getFileDetail")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.QUERY, description = "获取文件详情")
    public Result<List<FileChunkDetailVO>> getFileDetail(@NotNull(message = "文件ID不能为空") @RequestParam("fileId") Long fileId) {
        List<FileChunkDetailVO> fileChunkDetailList = fileService.getFileDetail(fileId);
        return Result.success(fileChunkDetailList);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.DELETE, description = "删除文件")
    public Result<Void> deleteFile(@NotNull(message = "文件ID不能为空") @RequestParam Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }

    /**
     * 文件上传初始化
     */
    @PostMapping("/init")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.UPLOAD, description = "初始化文件上传")
    public Result<UploadInitVO> initiateMultipartUpload(@RequestBody @Valid FileUploadChunkInitDTO fileUploadChunkInitDTO) {
        UploadInitVO uploadInitVO = fileService.initFileUpload(fileUploadChunkInitDTO);
        return Result.success(uploadInitVO);
    }

    /**
     * 获取上传进度
     */
    @GetMapping("/progress")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.QUERY, description = "获取上传进度", enabled = false)
    public Result<UploadProgressVO> getUploadProgress(@NotNull(message = "uploadId不能为空") @RequestParam String uploadId,
                                                      @NotNull(message = "fileKey不能为空") @RequestParam String fileKey) {
        return Result.success(fileService.getUploadProgress(uploadId, fileKey));
    }

    /**
     * 保存分片信息
     */
    @PostMapping("/chunk")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.UPLOAD, description = "上传文件分片", enabled = false)
    public Result<Void> uploadChunk(@RequestBody @Valid FileUploadChunkDTO fileUploadChunkDTO) {
        fileService.uploadChunk(fileUploadChunkDTO);
        return Result.success();
    }

    /**
     * 完成分片上传，进行合并
     */
    @PostMapping("/complete")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.UPLOAD, description = "完成文件上传")
    public Result<UploadCompleteVO> completeChunkUpload(@RequestBody FileUploadCompleteDTO fileUploadCompleteDTO) {
        UploadCompleteVO uploadCompleteVO = fileService.completeChunkUpload(fileUploadCompleteDTO);
        return Result.success(uploadCompleteVO);
    }

    /**
     * 暂停上传
     */
    @PostMapping("/pause")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.OTHER, description = "暂停上传")
    public Result<Void> pauseUpload(@RequestBody @Valid FileUploadPauseDTO fileUploadPauseDTO) {
        fileService.pauseUpload(fileUploadPauseDTO);
        return Result.success();
    }

    /**
     * 恢复上传
     */
    @PostMapping("/resume")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.UPLOAD, description = "恢复上传")
    public Result<UploadInitVO> resumeUpload(@RequestBody @Valid FileUploadPauseDTO fileUploadPauseDTO) {
        UploadInitVO uploadInitVO = fileService.resumeUpload(fileUploadPauseDTO);
        return Result.success(uploadInitVO);
    }

    /**
     * 取消上传
     */
    @PostMapping("/cancel")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.OTHER, description = "取消上传")
    public Result<Void> cancelUpload(@RequestBody FileUploadCancelDTO fileUploadCancelDTO) {
        fileService.cancelUpload(fileUploadCancelDTO);
        return Result.success();
    }

    /**
     * 文件下载
     */
    @GetMapping("/download")
    @OperationLogRecord(moduleName = "文件管理", operationType = OperationType.DOWNLOAD, description = "下载文件")
    public Result<String> downloadFile(@NotNull(message = "文件ID不能为为空") @RequestParam Long id) {
        return Result.success(fileService.downloadFile(id));
    }
}
