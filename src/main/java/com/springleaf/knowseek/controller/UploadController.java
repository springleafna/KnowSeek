package com.springleaf.knowseek.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.config.OssConfig;
import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadCompleteDTO;
import com.springleaf.knowseek.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class UploadController {

    private final FileService fileService;
    private final OssConfig ossConfig;
    private final OSS ossClient;

    /**
     * 分片上传初始化
     */
    @PostMapping("/init")
    public Result<String> initiateMultipartUpload(@RequestBody FileUploadChunkDTO fileUploadChunkDTO) {
        try {
            String fileName = fileUploadChunkDTO.getFileName();

            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                    ossConfig.getBucketName(), fileName);

            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);

            // 只返回uploadId，其他信息前端都有
            String uploadId = result.getUploadId();

            return Result.success(uploadId);
        } catch (Exception e) {
            return Result.error("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/chunk")
    public Result<String> uploadChunk(
            @RequestParam("fileName") String fileName,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunk") MultipartFile chunk) {  // 使用@RequestParam接收文件

        try {
            // 创建上传分片请求
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(ossConfig.getBucketName());
            uploadPartRequest.setKey(fileName);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setPartNumber(chunkIndex);
            uploadPartRequest.setInputStream(chunk.getInputStream());
            uploadPartRequest.setPartSize(chunk.getSize());

            // 上传分片到OSS
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);

            // 返回分片上传结果
            return Result.success(uploadPartResult.getETag());
        } catch (Exception e) {
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 完成分片上传
     */
    @PostMapping("/complete")
    public Result<String> completeMultipartUpload(@RequestBody FileUploadCompleteDTO fileUploadCompleteDTO) {
        try {
            // 【关键】打印整个接收到的对象
            log.info("收到完成上传请求 (原始DTO): {}", fileUploadCompleteDTO);

            String fileName = fileUploadCompleteDTO.getFileName();
            List<FileUploadCompleteDTO.PartETagInfo> partETagInfos = fileUploadCompleteDTO.getPartETagInfos();
            String uploadId = fileUploadCompleteDTO.getUploadId();

            // 【关键】检查列表和其内容
            log.info("准备转换 PartETagInfo 列表, 大小: {}", partETagInfos != null ? partETagInfos.size() : "null");
            if (partETagInfos != null) {
                for (int i = 0; i < partETagInfos.size(); i++) {
                    FileUploadCompleteDTO.PartETagInfo info = partETagInfos.get(i);
                    // 【关键】打印每个 PartETagInfo 对象的详细信息
                    log.info("PartETagInfo[{}]: partNumber={}, eTag={}, info.toString()='{}'",
                            i, info.getPartNumber(), info.getETag(), info.toString());
                    // 【关键】检查是否有 null 值
                    if (info.getETag() == null) {
                        log.error("发现 ETag 为 null 的 PartETagInfo: {}", info);
                        return Result.error("客户端传递了无效的分片信息 (ETag 为空)");
                    }
                    if (info.getPartNumber() == null) {
                        log.error("发现 partNumber 为 null 的 PartETagInfo: {}", info);
                        return Result.error("客户端传递了无效的分片信息 (partNumber 为空)");
                    }
                }
            } else {
                log.error("客户端传递的 partETagInfos 为 null");
                return Result.error("客户端未传递分片信息");
            }

            // 将PartETagInfo转换为OSS SDK的PartETag
            List<PartETag> partTags = partETagInfos.stream()
                    .map(info -> {
                        // 【关键】在创建 PartETag 前再次记录
                        log.debug("正在创建 OSS PartETag: partNumber={}, eTag={}", info.getPartNumber(), info.getETag());
                        PartETag pt = new PartETag(info.getPartNumber(), info.getETag());
                        // 【关键】创建后检查 OSS PartETag
                        log.debug("创建的 OSS PartETag: partNumber={}, eTag={}, getPartNumber()={}, getETag()='{}'",
                                info.getPartNumber(), info.getETag(), pt.getPartNumber(), pt.getETag());
                        return pt;
                    })
                    .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                    .collect(Collectors.toList());

            log.info("转换后的 OSS PartETag 列表: {}", partTags);

            // 创建完成上传请求
            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(ossConfig.getBucketName(),
                            fileName, uploadId, partTags);

            // 完成分片上传（合并分片）
            CompleteMultipartUploadResult result =
                    ossClient.completeMultipartUpload(completeRequest);

            // 返回最终结果
            return Result.success(result.getLocation());
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            return Result.error("完成分片上传失败: " + e.getMessage());
        }
    }
}
