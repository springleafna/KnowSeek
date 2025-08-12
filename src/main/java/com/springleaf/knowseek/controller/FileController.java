package com.springleaf.knowseek.controller;

import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.dto.FileUploadCompleteDTO;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 文件上传初始化
     */
    @PostMapping("/init")
    public Result<UploadInitVO> initiateMultipartUpload(@RequestBody @Valid FileUploadChunkInitDTO fileUploadChunkInitDTO) {
        UploadInitVO uploadInitVO = fileService.initFileUpload(fileUploadChunkInitDTO);
        return Result.success(uploadInitVO);
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/chunk")
    public Result<String> uploadChunk(@RequestBody @Valid FileUploadChunkDTO fileUploadChunkDTO) throws IOException {
        String eTag = fileService.uploadChunk(fileUploadChunkDTO);
        return Result.success(eTag);
    }

    /**
     * 完成分片上传，进行合并
     */
    @PostMapping("/complete")
    public Result<String> completeChunkUpload(@RequestBody FileUploadCompleteDTO fileUploadCompleteDTO) {
        String location = fileService.completeChunkUpload(fileUploadCompleteDTO);
        return Result.success(location);
    }

    /**
     * 暂停上传
     */
    @PostMapping("/pause")
    public Result<?> pauseUpload(@RequestBody FileUploadPauseDTO fileUploadPauseDTO) {
        return Result.success();
    }

    /**
     * 取消上上传
     */
    @PostMapping("/cancel")
    public Result<?> cancelUpload(@RequestBody FileUploadPauseDTO fileUploadPauseDTO) {
        return Result.success();
    }
}