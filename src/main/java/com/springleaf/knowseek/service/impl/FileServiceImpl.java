package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.springleaf.knowseek.config.OssConfig;
import com.springleaf.knowseek.constans.RedisKeyConstant;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssConfig ossConfig;
    private final OSS ossClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final FileUploadMapper fileUploadMapper;


    /**
     * 支持的文档类型扩展名（可以被Apache Tika解析并向量化的文件类型）
     */
    private static final Set<String> SUPPORTED_DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 文档类型
            "pdf",          // PDF文档
            "doc", "docx",  // Microsoft Word文档
            "xls", "xlsx",  // Microsoft Excel表格
            "ppt", "pptx",  // Microsoft PowerPoint演示文稿
            "txt",          // 纯文本文件
            "rtf",          // 富文本格式
            "md",           // Markdown文档

            // OpenDocument格式
            "odt",          // OpenDocument文本文档
            "ods",          // OpenDocument电子表格
            "odp",          // OpenDocument演示文稿

            // 网页和标记语言
            "html", "htm",  // HTML文档
            "xml",          // XML文档
            "json",         // JSON文件
            "csv",          // CSV文件

            // 电子书格式
            "epub",         // EPUB电子书

            // 其他文档格式
            "pages",        // Apple Pages文档
            "numbers",      // Apple Numbers表格
            "keynote"       // Apple Keynote演示文稿
    ));

    /**
     * 不支持的文件类型扩展名（无法有效解析文本内容的文件类型）
     */
    private static final Set<String> UNSUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 图片文件
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "tiff", "ico", "psd",

            // 音频文件
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a",

            // 视频文件
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp",

            // 压缩包
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz",

            // 可执行文件
            "exe", "msi", "dmg", "pkg", "deb", "rpm",

            // 字体文件
            "ttf", "otf", "woff", "woff2", "eot",

            // CAD文件
            "dwg", "dxf", "step", "iges",

            // 数据库文件
            "db", "sqlite", "mdb", "accdb",

            // 其他二进制文件
            "bin", "dat", "iso", "img"
    ));

    @Override
    public void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO) {
        // 文件类型验证（仅在第一个分片时进行验证）
        if (fileUploadChunkDTO.getChunkIndex() == 0) {
            validateFileType(fileUploadChunkDTO.getFileName());
        }
        // 获取文件类型信息
        String fileType = getFileType(fileUploadChunkDTO.getFileName());
        String contentType = fileUploadChunkDTO.getFile().getContentType();

    }

    @Override
    public UploadInitVO initFileUpload(FileUploadChunkInitDTO fileUploadChunkInitDTO) {
        String fileName = fileUploadChunkInitDTO.getFileName();
        String fileMd5 = fileUploadChunkInitDTO.getFileMd5();
        Long userId = StpUtil.getLoginIdAsLong();
        // 根据文件Md5值和用户ID判断文件是否以及被上传成功（秒传逻辑）
        if (fileUploadMapper.existFileUpload(fileMd5, userId, UploadStatusEnum.COMPLETED.getStatus())) {
            log.info("fileName:{}，该文件已经被上传成功，支持秒传", fileName);
            return new UploadInitVO(true, null);
        }
        // 未秒传，进行文件上传OSS初始化
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                ossConfig.getBucketName(), fileName);

        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);

        // 获取文件上传初始化的uploadId
        String uploadId = result.getUploadId();

        // 将文件上传信息保存到数据库，设置上传状态为上传中
        FileUpload fileUpload = new FileUpload();
        fileUpload.setFileName(fileName);
        fileUpload.setFileMd5(fileMd5);
        fileUpload.setStatus(UploadStatusEnum.UPLOADING.getStatus());
        fileUpload.setUserId(userId);
        if (fileUploadMapper.saveFileUpload(fileUpload) < 1) {
            throw new BusinessException("保存文件上传信息失败");
        }

        // 将文件上传信息存入Redis
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String nowStr = now.format(formatter);

        String redisKey = RedisKeyConstant.FILE_UPLOAD_INIT_KEY.formatted(uploadId);
        Map<String, String> redisValue = new HashMap<>();
        redisValue.put("fileName", fileName);
        redisValue.put("fileMd5", fileMd5);
        redisValue.put("userId", String.valueOf(userId));
        redisValue.put("initTime", nowStr);

        stringRedisTemplate.opsForHash().putAll(redisKey, redisValue);

        return new UploadInitVO(false, uploadId);
    }

    /**
     * 验证文件类型是否支持
     * @param fileName 文件名
     */
    public void validateFileType(String fileName) {
        log.debug("开始验证文件类型: fileName={}", fileName);

        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件名为空或null");
            throw new BusinessException("文件名不能为空");
        }

        // 提取文件扩展名
        String extension = extractFileExtension(fileName);
        if (extension == null) {
            log.warn("无法提取文件扩展名: fileName={}", fileName);
            throw new BusinessException("文件必须有扩展名");
        }

        String fileType = getFileTypeDescription(extension);
        log.debug("文件类型识别结果: fileName={}, extension={}, fileType={}", fileName, extension, fileType);

        // 检查是否为支持的文档类型
        if (SUPPORTED_DOCUMENT_EXTENSIONS.contains(extension)) {
            log.info("文件类型验证通过: fileName={}, extension={}, fileType={}", fileName, extension, fileType);
            throw new BusinessException("支持的文件类型");
        }

        // 检查是否为明确不支持的类型
        if (UNSUPPORTED_EXTENSIONS.contains(extension)) {
            String message = String.format("不支持的文件类型：%s。系统仅支持文档类型文件的解析和向量化", fileType);
            log.warn("文件类型验证失败: fileName={}, extension={}, fileType={}, reason=unsupported_type",
                    fileName, extension, fileType);
            throw new BusinessException(message);
        }

        // 对于未知的文件类型，给出提示
        String message = String.format("未知的文件类型：%s。建议使用支持的文档格式（如PDF、Word、Excel、PowerPoint、文本文件等）", fileType);
        log.warn("文件类型验证失败: fileName={}, extension={}, fileType={}, reason=unknown_type",
                fileName, extension, fileType);
        throw new BusinessException(message);
    }

    /**
     * 提取文件扩展名
     *
     * @param fileName 文件名
     * @return 小写的文件扩展名，如果没有扩展名则返回null
     */
    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 根据文件扩展名获取文件类型描述
     *
     * @param extension 文件扩展名
     * @return 文件类型描述
     */
    private String getFileTypeDescription(String extension) {
        if (extension == null) {
            return "unknown";
        }

        // 根据文件扩展名返回文件类型
        switch (extension.toLowerCase()) {
            case "pdf":
                return "PDF文档";
            case "doc":
            case "docx":
                return "Word文档";
            case "xls":
            case "xlsx":
                return "Excel表格";
            case "ppt":
            case "pptx":
                return "PowerPoint演示文稿";
            case "txt":
                return "文本文件";
            case "rtf":
                return "富文本文档";
            case "md":
                return "Markdown文档";
            case "odt":
                return "OpenDocument文本";
            case "ods":
                return "OpenDocument表格";
            case "odp":
                return "OpenDocument演示文稿";
            case "html":
            case "htm":
                return "HTML文档";
            case "xml":
                return "XML文档";
            case "json":
                return "JSON文件";
            case "csv":
                return "CSV文件";
            case "epub":
                return "EPUB电子书";
            case "pages":
                return "Apple Pages文档";
            case "numbers":
                return "Apple Numbers表格";
            case "keynote":
                return "Apple Keynote演示文稿";
            case "jpg":
            case "jpeg":
                return "JPEG图片";
            case "png":
                return "PNG图片";
            case "gif":
                return "GIF图片";
            case "bmp":
                return "BMP图片";
            case "svg":
                return "SVG图片";
            case "mp4":
                return "MP4视频";
            case "avi":
                return "AVI视频";
            case "mov":
                return "MOV视频";
            case "mp3":
                return "MP3音频";
            case "wav":
                return "WAV音频";
            case "zip":
                return "ZIP压缩包";
            case "rar":
                return "RAR压缩包";
            case "7z":
                return "7Z压缩包";
            default:
                return extension.toUpperCase() + "文件";
        }
    }

    /**
     * 获取支持的文件类型列表（用于前端显示）
     *
     * @return 支持的文件类型描述列表
     */
    public Set<String> getSupportedFileTypes() {
        Set<String> supportedTypes = new HashSet<>();
        for (String extension : SUPPORTED_DOCUMENT_EXTENSIONS) {
            supportedTypes.add(getFileTypeDescription(extension));
        }
        return supportedTypes;
    }

    /**
     * 获取支持的文件扩展名列表
     *
     * @return 支持的文件扩展名集合
     */
    public Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_DOCUMENT_EXTENSIONS);
    }

    /**
     * 根据文件名获取文件类型
     *
     * @param fileName 文件名
     * @return 文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "unknown";
        }

        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();

        // 根据文件扩展名返回文件类型
        switch (extension) {
            case "pdf":
                return "PDF文档";
            case "doc":
            case "docx":
                return "Word文档";
            case "xls":
            case "xlsx":
                return "Excel表格";
            case "ppt":
            case "pptx":
                return "PowerPoint演示文稿";
            case "txt":
                return "文本文件";
            case "md":
                return "Markdown文档";
            case "jpg":
            case "jpeg":
                return "JPEG图片";
            case "png":
                return "PNG图片";
            case "gif":
                return "GIF图片";
            case "bmp":
                return "BMP图片";
            case "svg":
                return "SVG图片";
            case "mp4":
                return "MP4视频";
            case "avi":
                return "AVI视频";
            case "mov":
                return "MOV视频";
            case "wmv":
                return "WMV视频";
            case "mp3":
                return "MP3音频";
            case "wav":
                return "WAV音频";
            case "flac":
                return "FLAC音频";
            case "zip":
                return "ZIP压缩包";
            case "rar":
                return "RAR压缩包";
            case "7z":
                return "7Z压缩包";
            case "tar":
                return "TAR压缩包";
            case "gz":
                return "GZ压缩包";
            case "json":
                return "JSON文件";
            case "xml":
                return "XML文件";
            case "csv":
                return "CSV文件";
            case "html":
            case "htm":
                return "HTML文件";
            case "css":
                return "CSS文件";
            case "js":
                return "JavaScript文件";
            case "java":
                return "Java源码";
            case "py":
                return "Python源码";
            case "cpp":
            case "c":
                return "C/C++源码";
            case "sql":
                return "SQL文件";
            default:
                return extension.toUpperCase() + "文件";
        }
    }
}