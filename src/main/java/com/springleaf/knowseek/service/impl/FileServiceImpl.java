package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.springleaf.knowseek.config.OssConfig;
import com.springleaf.knowseek.constans.OssUserFilePathConstant;
import com.springleaf.knowseek.constans.RedisKeyConstant;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.FileUploadMapper;
import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.dto.FileUploadCompleteDTO;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.UploadCompleteVO;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.model.vo.UploadProgressVO;
import com.springleaf.knowseek.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    public UploadInitVO initFileUpload(FileUploadChunkInitDTO dto) {
        String fileName = dto.getFileName();
        // TODO:验证文件类型是否支持
        // validateFileType(fileName);

        String fileMd5 = dto.getFileMd5();
        Long fileSize = dto.getFileSize();
        Integer chunkTotal = dto.getChunkTotal();
        Long userId = StpUtil.getLoginIdAsLong();
        // URL 过期时间配置化
        long expireSeconds = ossConfig.getPresignedUrlExpiration();
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        log.debug("开始初始化文件上传，用户ID: {}, 文件名: {}, 文件MD5: {}", userId, fileName, fileMd5);

        try {
            // 根据文件Md5值和用户ID判断文件是否以及被上传成功（秒传逻辑）
            FileUpload fileUpload = fileUploadMapper.existFileUpload(fileMd5, userId, UploadStatusEnum.COMPLETED.getStatus());
            if (fileUpload != null) {
                log.info("fileName:{}，该文件已经被上传成功，支持秒传", fileName);
                return new UploadInitVO(true, null, fileUpload.getLocation(), null);
            }

            // TODO：最好先保存上传信息到数据库再调用OSS初始化，防止数据库插入失败，uploadId 已在 OSS 存在，但无记录 → 成为“孤儿上传任务”。
            // TODO：启动定时任务扫描数据库和Redis并中止“超时未完成”的 uploadId
            // TODO：未处理 OSS 异常重试机制，ossClient.initiateMultipartUpload() 可能因网络抖动失败。
            // 未秒传，进行文件上传OSS初始化
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                    ossConfig.getBucketName(), fileName);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            String uploadId = result.getUploadId();
            log.debug("OSS分片上传初始化成功，uploadId: {}", uploadId);

            // 生成每个分片的直传 URL，返回预签名的 直传签名 URL，允许客户端在一段时间内直接向 OSS 上传分片
            Map<Integer, String> uploadUrls = new HashMap<>();
            for (int i = 1; i <= dto.getChunkTotal(); i++) {
                GeneratePresignedUrlRequest urlRequest =
                        new GeneratePresignedUrlRequest(ossConfig.getBucketName(), fileName, HttpMethod.PUT);
                urlRequest.setExpiration(expiration);
                // 把 uploadId 和 partNumber 作为 query 参数
                Map<String, String> queryParams = new HashMap<>(2);
                queryParams.put("uploadId", uploadId);
                queryParams.put("partNumber", String.valueOf(i));
                urlRequest.setQueryParameter(queryParams);

                URL signedUrl = ossClient.generatePresignedUrl(urlRequest);
                uploadUrls.put(i, signedUrl.toString());
            }

            // 将文件上传信息保存到数据库，设置上传状态为上传中
            fileUpload = new FileUpload();
            fileUpload.setFileName(fileName);
            fileUpload.setFileMd5(fileMd5);
            fileUpload.setStatus(UploadStatusEnum.UPLOADING.getStatus());
            fileUpload.setUserId(userId);
            fileUpload.setTotalSize(fileSize);
            // TODO:对于是否公开需要用户自行设置 或者 取消这个字段
            fileUpload.setIsPublic(false);
            int saveResult = fileUploadMapper.saveFileUpload(fileUpload);
            if (saveResult < 1) {
                throw new BusinessException("保存文件上传信息失败");
            }
            log.debug("文件上传信息已保存到数据库，上传状态设置为上传中");

            // 将文件上传信息存入Redis
            String fileUploadInfoKey = String.format(RedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);
            Map<String, String> redisValue = new HashMap<>();
            redisValue.put("id", String.valueOf(fileUpload.getId()));
            redisValue.put("fileName", fileName);
            redisValue.put("extension", getFileExtension(fileName));
            redisValue.put("fileMd5", fileMd5);
            redisValue.put("userId", String.valueOf(userId));
            redisValue.put("chunkTotal", String.valueOf(chunkTotal));

            stringRedisTemplate.opsForHash().putAll(fileUploadInfoKey, redisValue);
            stringRedisTemplate.expire(fileUploadInfoKey, expireSeconds, TimeUnit.SECONDS);
            log.debug("文件上传信息已存入Redis，key: {}", fileUploadInfoKey);

            return new UploadInitVO(false, uploadId, null, uploadUrls);
        } catch (Exception e) {
            log.error("初始化文件上传失败，文件名: {}, 文件MD5: {}, 错误信息: {}", fileName, fileMd5, e.getMessage());
            throw new BusinessException("初始化文件上传失败，请重试");
        }
    }

    @Override
    public String uploadChunk(FileUploadChunkDTO fileUploadChunkDTO) {
        try {
            String uploadId = fileUploadChunkDTO.getUploadId();
            Integer chunkIndex = fileUploadChunkDTO.getChunkIndex();
            String eTag = fileUploadChunkDTO.getETag();
            Long chunkSize = fileUploadChunkDTO.getChunkSize();

            // Redis Key
            String chunkInfoKey = String.format(RedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, chunkIndex);
            String chunkStatusKey = String.format(RedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
            String chunkETagKey = String.format(RedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
            String fileUploadInfoKey = String.format(RedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");
            // 保存分片信息
            Map<String, String> chunkInfo = new HashMap<>();
            chunkInfo.put("chunkSize", String.valueOf(chunkSize));
            chunkInfo.put("eTag", eTag);
            chunkInfo.put("fileName", fileName);

            // 设置分片信息
            stringRedisTemplate.opsForHash().putAll(chunkInfoKey, chunkInfo);

            // 设置分片上传状态（BitMap）
            stringRedisTemplate.opsForValue().setBit(chunkStatusKey, chunkIndex - 1, true);

            // 保存 ETag 到有序集合（ZSet，方便按序合并）
            stringRedisTemplate.opsForZSet().add(chunkETagKey, eTag, chunkIndex);

            log.debug("分片上报成功: uploadId={}, chunkIndex={}, eTag={}", uploadId, chunkIndex, eTag);

            return eTag;
        } catch (Exception e) {
            log.error("分片上报失败: {}", fileUploadChunkDTO, e);
            throw new BusinessException("分片上报失败: " + e.getMessage());
        }
    }


    @Override
    public UploadCompleteVO completeChunkUpload(FileUploadCompleteDTO fileUploadCompleteDTO) {
        try {
            log.info("进行分片合并: {}", fileUploadCompleteDTO);
            String fileName = fileUploadCompleteDTO.getFileName();
            String uploadId = fileUploadCompleteDTO.getUploadId();
            Integer chunkTotalSize = fileUploadCompleteDTO.getChunkTotalSize();

            String chunkStatusKey = String.format(RedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
            String chunkETagKey = String.format(RedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
            String fileUploadInfoKey = String.format(RedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

            // 1. 验证分片是否全部上传
            List<Integer> pendingChunkIndexList = new ArrayList<>();
            for (int i = 0; i < chunkTotalSize; i++) {
                // TODO：Bitmap 验证时的性能优化
                Boolean isUploaded = stringRedisTemplate.opsForValue().getBit(chunkStatusKey, i);
                // 记录未上传的分片索引
                if (Boolean.FALSE.equals(isUploaded)) {
                    pendingChunkIndexList.add(i);
                }
            }
            if (!pendingChunkIndexList.isEmpty()) {
                // 将未上传的分片返回给前端重新上传
                return new UploadCompleteVO(true, pendingChunkIndexList, null);
            }

            // TODO：需要在合并前加一个完整性比对（本地已上传分片数 vs OSS 已上传分片数），因为对 ETag 数据强依赖
            // 2. 获取 Redis 中 分片 ETag 有序列表进行合并
            // 从ZSet中按score（即chunkIndex）顺序获取所有ETag
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    stringRedisTemplate.opsForZSet().rangeWithScores(chunkETagKey, 0, -1);
            if (tuples.isEmpty()) {
                throw new BusinessException("未找到分片 ETag 信息，无法合并文件");
            }

            List<PartETag> partETags = tuples.stream()
                    .map(tuple -> new PartETag(tuple.getScore().intValue(), tuple.getValue()))
                    .toList();

            // 设置文件上传路径
            Object userId = StpUtil.getLoginId();
            String fileMd5 = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileMd5");
            String extension = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "extension");
            String timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String filePath = String.format(OssUserFilePathConstant.USER_FILE_PATH, userId, timestamp, fileMd5, extension);

            // 3. 执行 OSS 分片合并
            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(ossConfig.getBucketName(),
                            filePath, uploadId, partETags);

            // 完成分片上传（合并分片）
            CompleteMultipartUploadResult result;
            try {
                result = ossClient.completeMultipartUpload(completeRequest);
            } catch (Exception e) {
                throw new BusinessException("OSS 分片合并失败: " + e.getMessage());
            }
            String location = result.getLocation();

            // 4. 更新数据库记录
            // 获取该分片归属的文件的id
            Long id = (Long) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
            // 更新文件OSS路径
            if (fileUploadMapper.updateOSSLocation(id, location) < 1) {
                throw new BusinessException("文件OSS路径设置失败");
            }
            // 更新数据库该文件上传状态为上传完成
            // TODO：可能出现数据库更新失败，状态不一致（OSS 文件已经在，但 DB 状态没更新）
            if (fileUploadMapper.updateUploadStatus(id, UploadStatusEnum.COMPLETED.getStatus()) < 1) {
                throw new BusinessException("文件上传状态更新失败");
            }
            // 5. 删除 Redis 分片相关的缓存（批量删除）
            List<String> keysToDelete = new ArrayList<>();
            for (int i = 1; i <= chunkTotalSize; i++) {
                keysToDelete.add(String.format(RedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, i));
            }
            keysToDelete.add(chunkStatusKey);
            keysToDelete.add(chunkETagKey);
            keysToDelete.add(fileUploadInfoKey);
            stringRedisTemplate.delete(keysToDelete);

            log.info("合并完成，文件名：{}，uploadId：{}，文件地址：{}", fileName, uploadId, location);
            // TODO：合并完成后需要根据 location 地址下载文件并发送 mq 消息进行文件的向量化处理

            return new UploadCompleteVO(false, null, location);
        } catch (Exception e) {
            log.error("分片合并失败", e);
            throw new BusinessException("分片合并失败: " + e.getMessage());
        }
    }

    @Override
    public UploadProgressVO getUploadProgress(String uploadId, String fileKey) {
        String chunkStatusKey = String.format(RedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
        String chunkETagKey = String.format(RedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);

        // 先从 Redis 中判断分片是否全部上传

        List<Integer> uploadedParts = new ArrayList<>();
        // 调用阿里云 ListParts 接口 获取当前 UploadId 下已经成功上传的分片列表
        try {
            // 调用阿里云 OSS 的 ListParts 接口
            ListPartsRequest listPartsRequest = new ListPartsRequest(ossConfig.getBucketName(), fileKey, uploadId);
            PartListing partListing = ossClient.listParts(listPartsRequest);

            // 提取已上传的 PartNumber
            for (PartSummary part : partListing.getParts()) {
                uploadedParts.add(part.getPartNumber());
            }

            // 排序，便于前端查看
            uploadedParts.sort(Integer::compareTo);

        } catch (OSSException e) {
            if ("NoSuchUpload".equals(e.getErrorCode())) {
                // uploadId 不存在或已过期，视为“无分片上传”
                uploadedParts.clear();
            } else {
                throw e; // 其他错误抛出
            }
        }

        return new UploadProgressVO(uploadedParts);
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

        // 获取文件类型
        String fileType = getFileTypeDescription(extension);
        log.debug("文件类型识别结果: fileName={}, extension={}, fileType={}", fileName, extension, fileType);

        // 检查是否为支持的文档类型
        if (SUPPORTED_DOCUMENT_EXTENSIONS.contains(extension)) {
            log.info("文件类型验证通过: fileName={}, extension={}, fileType={}", fileName, extension, fileType);
            // TODO:这里和下面不应该抛异常
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

    /**
     * 根据文件名获取文件扩展名 .xxx
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        // 确保点号在最后一个路径分隔符之后（处理类似 "path/to.file/name" 的情况）
        if (lastDotIndex > lastSeparatorIndex && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex).toLowerCase();
        }

        return "";
    }
}