package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.springleaf.knowseek.common.Result;
import com.springleaf.knowseek.config.OssConfig;
import com.springleaf.knowseek.constans.RedisKeyConstant;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.dto.FileUploadCompleteDTO;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    public void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO) throws IOException {
        // 文件类型验证（仅在第一个分片时进行验证）
        if (fileUploadChunkDTO.getChunkIndex() == 0) {
            validateFileType(fileUploadChunkDTO.getFileName());
        }
        // 获取文件类型信息
        String fileType = getFileType(fileUploadChunkDTO.getFileName());
        String contentType = fileUploadChunkDTO.getFile().getContentType();

        String uploadId = fileUploadChunkDTO.getUploadId();
        Integer chunkIndex = fileUploadChunkDTO.getChunkIndex();
        String fileName = fileUploadChunkDTO.getFileName();
        MultipartFile chunkFile = fileUploadChunkDTO.getFile();

        // 创建上传分片请求
        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setBucketName(ossConfig.getBucketName());
        uploadPartRequest.setKey(fileName);
        uploadPartRequest.setUploadId(uploadId);
        uploadPartRequest.setPartNumber(chunkIndex);
        uploadPartRequest.setInputStream(chunkFile.getInputStream());
        uploadPartRequest.setPartSize(chunkFile.getSize());

        // 上传分片到OSS
        UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
        // 获取eTag
        String eTag = uploadPartResult.getETag();

        // 将分片信息保存到Redis中
        String chunkKey = String.format(RedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, chunkIndex);
        stringRedisTemplate.opsForHash().put(chunkKey, "eTag", eTag);
        stringRedisTemplate.opsForHash().putIfAbsent(chunkKey, "fileName", fileName);
        stringRedisTemplate.opsForHash().putIfAbsent(chunkKey, "contentType", contentType);
        stringRedisTemplate.opsForHash().putIfAbsent(chunkKey, "fileType", fileType);

        // 设置Redis BitMap 中该分片的状态为已上传
        String chunkStatusKey = String.format(RedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
        stringRedisTemplate.opsForValue().setBit(chunkStatusKey, chunkIndex - 1, true);

        // 保存分片 ETag 有序列表
        String chunkETagKey = String.format(RedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
        stringRedisTemplate.opsForZSet().add(chunkETagKey, eTag, System.currentTimeMillis());
    }

    @Override
    public UploadInitVO initFileUpload(FileUploadChunkInitDTO fileUploadChunkInitDTO) {
        String fileName = fileUploadChunkInitDTO.getFileName();
        String fileMd5 = fileUploadChunkInitDTO.getFileMd5();
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("开始初始化文件上传，用户ID: {}, 文件名: {}, 文件MD5: {}", userId, fileName, fileMd5);

        try {
            // 根据文件Md5值和用户ID判断文件是否以及被上传成功（秒传逻辑）
            boolean exists = fileUploadMapper.existFileUpload(fileMd5, userId, UploadStatusEnum.COMPLETED.getStatus());
            if (exists) {
                log.info("fileName:{}，该文件已经被上传成功，支持秒传", fileName);
                return new UploadInitVO(true, null);
            }
            // 未秒传，进行文件上传OSS初始化
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                    ossConfig.getBucketName(), fileName);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            String uploadId = result.getUploadId();
            log.debug("初始化OSS多部分上传成功，uploadId: {}", uploadId);

            // 将文件上传信息保存到数据库，设置上传状态为上传中
            FileUpload fileUpload = new FileUpload();
            fileUpload.setFileName(fileName);
            fileUpload.setFileMd5(fileMd5);
            fileUpload.setStatus(UploadStatusEnum.UPLOADING.getStatus());
            fileUpload.setUserId(userId);
            int saveResult = fileUploadMapper.saveFileUpload(fileUpload);
            if (saveResult < 1) {
                throw new BusinessException("保存文件上传信息失败");
            }
            log.debug("文件上传信息已保存到数据库，上传状态设置为上传中");

            // 将文件上传信息存入Redis
            String redisKey = String.format(RedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);
            Map<String, String> redisValue = new HashMap<>();
            redisValue.put("fileName", fileName);
            redisValue.put("fileMd5", fileMd5);
            redisValue.put("userId", String.valueOf(userId));

            stringRedisTemplate.opsForHash().putAll(redisKey, redisValue);
            log.debug("文件上传信息已存入Redis，key: {}", redisKey);

            return new UploadInitVO(false, uploadId);
        } catch (Exception e) {
            log.error("初始化文件上传失败，文件名: {}, 文件MD5: {}, 错误信息: {}", fileName, fileMd5, e.getMessage());
            throw new BusinessException("初始化文件上传失败，请重试");
        }
    }

    @Override
    public String completeChunkUpload(FileUploadCompleteDTO fileUploadCompleteDTO) {
        try {
            log.info("进行分片合并: {}", fileUploadCompleteDTO);

            // TODO：验证 Redis BitMap 该文件分片是否都已上传


            // TODO： 获取 Redis 中 分片 ETag 有序列表进行合并

            String fileName = fileUploadCompleteDTO.getFileName();
            List<FileUploadCompleteDTO.PartETagInfo> partETagInfos = fileUploadCompleteDTO.getPartETagInfos();
            String uploadId = fileUploadCompleteDTO.getUploadId();

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
                        throw new BusinessException("客户端传递了无效的分片信息 (ETag 为空)");
                    }
                    if (info.getPartNumber() == null) {
                        log.error("发现 partNumber 为 null 的 PartETagInfo: {}", info);
                        throw new BusinessException("客户端传递了无效的分片信息 (partNumber 为空)");
                    }
                }
            } else {
                log.error("客户端传递的 partETagInfos 为 null");
                throw new BusinessException("客户端未传递分片信息");
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
            CompleteMultipartUploadResult completeUploadResult =
                    ossClient.completeMultipartUpload(completeRequest);
            String location = completeUploadResult.getLocation();

            // TODO：设置数据库该文件下载路径

            // TODO：设置数据库该文件上传状态为上传完成

            // TODO：清除 Redis 中相关分片信息

            // 返回最终结果
            return location;
        } catch (Exception e) {
            log.error("分片合并失败", e);
            throw new BusinessException("分片合并失败: " + e.getMessage());
        }
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