package com.springleaf.knowseek.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.config.OssConfig;
import com.springleaf.knowseek.constans.OssUserFileKeyConstant;
import com.springleaf.knowseek.constans.UploadRedisKeyConstant;
import com.springleaf.knowseek.enums.UploadStatusEnum;
import com.springleaf.knowseek.exception.BusinessException;
import com.springleaf.knowseek.mapper.mysql.FileUploadMapper;
import com.springleaf.knowseek.model.bo.VectorBO;
import com.springleaf.knowseek.model.domain.FileWithKbNameDO;
import com.springleaf.knowseek.model.dto.*;
import com.springleaf.knowseek.model.entity.FileUpload;
import com.springleaf.knowseek.model.vo.FileItemVO;
import com.springleaf.knowseek.model.vo.UploadCompleteVO;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.model.vo.UploadProgressVO;
import com.springleaf.knowseek.mq.event.FileVectorizeEvent;
import com.springleaf.knowseek.mq.producer.EventPublisher;
import com.springleaf.knowseek.service.FileService;
import com.springleaf.knowseek.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssConfig ossConfig;
    private final OSS ossClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final FileUploadMapper fileUploadMapper;
    private final EventPublisher eventPublisher;
    private final FileVectorizeEvent fileVectorizeEvent;

    @Override
    public PageInfo<FileItemVO> getFileList(FilePageDTO filePageDTO) {
        long userId = StpUtil.getLoginIdAsLong();

        PageHelper.startPage(filePageDTO.getPageNum(), filePageDTO.getPageSize());

        String sortOrder = "asc".equalsIgnoreCase(filePageDTO.getSortOrder()) ? "asc" : "desc";

        // 执行查询（模糊搜索条件已在XML中处理）
        List<FileWithKbNameDO> fileDOs = fileUploadMapper.selectPageWithKbName(
                userId,
                filePageDTO.getFileName(),
                filePageDTO.getKbName(),
                filePageDTO.getSortBy(),
                sortOrder
        );

        // 用查询结果创建PageInfo，此时包含了分页信息
        PageInfo<FileWithKbNameDO> doPageInfo = new PageInfo<>(fileDOs);

        // 将 List<FileWithKbNameDO> 转换为 List<FileItemVO>
        List<FileItemVO> voList = doPageInfo.getList().stream()
                .map(fileDO -> {
                    FileItemVO fileItemVO = new FileItemVO();
                    BeanUtils.copyProperties(fileDO, fileItemVO);

                    // 根据文件名设置文件类型
                    fileItemVO.setType(FileUtil.extractFileExtension(fileDO.getFileName()));

                    // 格式化文件大小
                    fileItemVO.setTotalSize(FileUtil.formatFileSize(fileDO.getTotalSize()));

                    // 转换状态
                    UploadStatusEnum statusEnum = UploadStatusEnum.getByStatus(fileDO.getStatus());
                    fileItemVO.setStatus(statusEnum != null ? statusEnum.getDescription() : "未知状态");

                    // 4. 设置知识库名称
                    fileItemVO.setKnowledgeBaseName(fileDO.getKnowledgeBaseName());

                    return fileItemVO;
                })
                .collect(Collectors.toList());

        // 创建VO的PageInfo并返回
        PageInfo<FileItemVO> voPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(doPageInfo, voPageInfo);
        voPageInfo.setList(voList);

        return voPageInfo;
    }

    @Override
    public UploadInitVO initFileUpload(FileUploadChunkInitDTO dto) {
        String fileName = dto.getFileName();
        // 验证文件类型是否支持
        FileUtil.validateFileType(fileName);

        Long userId = StpUtil.getLoginIdAsLong();
        String fileMd5 = dto.getFileMd5();
        Long fileSize = dto.getFileSize();  //文件大小
        Integer chunkTotal = dto.getChunkTotal();   // 分片总数
        Long knowledgeBaseId = dto.getKnowledgeBaseId();
        String extension = FileUtil.extractFileExtension(fileName);  // 获取文件扩展名：xxx
        // URL 过期时间配置化
        long expireSeconds = ossConfig.getPresignedUrlExpiration();
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        log.info("开始初始化文件上传，用户ID: {}, 文件名: {}, 文件MD5: {}, 文件大小：{}, 分片总数：{}", userId, fileName, fileMd5, fileSize, chunkTotal);

        try {
            // 根据文件Md5值和用户ID和知识库ID判断文件是否以及被上传成功（秒传逻辑）
            FileUpload fileUpload = fileUploadMapper.existFileUpload(fileMd5, userId, knowledgeBaseId);
            if (fileUpload != null) {
                Integer status = fileUpload.getStatus();
                if (status.equals(UploadStatusEnum.COMPLETED.getStatus())) {
                    // 前端提示用户文件已存在
                    log.info("fileName:{}，该文件已经被上传成功，支持秒传", fileName);
                    return new UploadInitVO(status, null, fileUpload.getLocation(), null);
                }
                // 其他情况：前端做不同提示
                return new UploadInitVO(status, null, null, null);
            }

            // 设置文件上传路径
            String timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);    // 获取当前时间：如20250823
            String fileKey = String.format(OssUserFileKeyConstant.USER_UPLOAD_FILE_KEY, userId, knowledgeBaseId, timestamp, fileMd5, extension);

            // 未秒传，进行文件上传OSS初始化
            String uploadId;
            Map<Integer, String> uploadUrls = new HashMap<>();  // 每个分片的预签名，传给前端进行阿里云OSS上传
            try {
                InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                        ossConfig.getBucketName(), fileKey);
                InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
                uploadId = result.getUploadId();
                log.info("OSS分片上传初始化成功，uploadId: {}", uploadId);

                // 生成每个分片的直传 URL，返回预签名的 直传签名 URL，允许客户端在一段时间内直接向 OSS 上传分片
                for (int i = 1; i <= dto.getChunkTotal(); i++) {
                    GeneratePresignedUrlRequest urlRequest =
                            new GeneratePresignedUrlRequest(ossConfig.getBucketName(), fileKey, HttpMethod.PUT);
                    urlRequest.setExpiration(expiration);
                    // 把 uploadId 和 partNumber 作为 query 参数
                    Map<String, String> queryParams = new HashMap<>(2);
                    queryParams.put("uploadId", uploadId);
                    queryParams.put("partNumber", String.valueOf(i));
                    urlRequest.setQueryParameter(queryParams);

                    URL signedUrl = ossClient.generatePresignedUrl(urlRequest);
                    uploadUrls.put(i, signedUrl.toString());
                }
            } catch (OSSException e) {
                // OSS 服务端异常：如权限不足、Bucket 不存在、文件名非法等
                log.error("OSS服务端错误，初始化分片上传失败。Bucket: {}, fileKey: {}, Message: {}, ErrorCode: {}, RequestId: {}",
                        ossConfig.getBucketName(), fileKey, e.getMessage(), e.getErrorCode(), e.getRequestId(), e);
                throw new RuntimeException("文件上传初始化失败（OSS错误）: " + e.getMessage(), e);
            } catch (ClientException e) {
                // 客户端异常：如网络连接失败、SDK 内部错误、参数错误等
                log.error("客户端错误，初始化分片上传失败。Bucket: {}, fileKey: {}, Message: {}",
                        ossConfig.getBucketName(), fileKey, e.getMessage(), e);
                throw new RuntimeException("文件上传初始化失败（网络或客户端错误）: " + e.getMessage(), e);
            }

            // 将文件上传信息保存到数据库，设置上传状态为初始化
            fileUpload = new FileUpload();
            fileUpload.setFileName(fileName);
            fileUpload.setFileMd5(fileMd5);
            fileUpload.setStatus(UploadStatusEnum.INITIALIZED.getStatus());
            fileUpload.setUserId(userId);
            fileUpload.setTotalSize(fileSize);
            fileUpload.setKnowledgeBaseId(knowledgeBaseId);
            // TODO:对于是否公开需要用户自行设置 或者 取消这个字段
            fileUpload.setIsPublic(false);
            fileUploadMapper.saveFileUpload(fileUpload);
            log.info("文件上传信息已保存到数据库，上传状态设置为上传中");

            // 将文件上传信息存入Redis
            String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);
            Map<String, String> redisValue = new HashMap<>();
            redisValue.put("id", String.valueOf(fileUpload.getId()));
            redisValue.put("fileName", fileName);
            redisValue.put("fileKey", fileKey);
            redisValue.put("extension", extension);
            redisValue.put("fileMd5", fileMd5);
            redisValue.put("userId", String.valueOf(userId));
            redisValue.put("chunkTotal", String.valueOf(chunkTotal));
            redisValue.put("knowledgeBaseId", String.valueOf(knowledgeBaseId));
            // TODO：待替换真实值
            redisValue.put("organizationId", "123");

            stringRedisTemplate.opsForHash().putAll(fileUploadInfoKey, redisValue);
            stringRedisTemplate.expire(fileUploadInfoKey, expireSeconds, TimeUnit.SECONDS);
            log.info("文件上传信息已存入Redis，key: {}", fileUploadInfoKey);

            return new UploadInitVO(UploadStatusEnum.INITIALIZED.getStatus(), uploadId, null, uploadUrls);
        } catch (Exception e) {
            log.error("初始化文件上传失败，文件名: {}, 文件MD5: {}, 错误信息: {}", fileName, fileMd5, e.getMessage());
            throw new BusinessException("初始化文件上传失败，请重试");
        }
    }

    @Override
    public void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO) {
        try {
            String uploadId = fileUploadChunkDTO.getUploadId();
            Integer chunkIndex = fileUploadChunkDTO.getChunkIndex();
            String eTag = fileUploadChunkDTO.getETag();
            Long chunkSize = fileUploadChunkDTO.getChunkSize();

            // Redis Key
            String chunkInfoKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, chunkIndex);
            String chunkStatusKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
            String chunkETagKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
            String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");
            if (chunkIndex == 1) {
                // 上传第一个分片时将数据库文件上传状态设置为上传中
                String idStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
                if (idStr == null) {
                    throw new BusinessException("上传文件ID不存在");
                }
                Long fileId = Long.valueOf(idStr);
                fileUploadMapper.updateUploadStatus(fileId, UploadStatusEnum.UPLOADING.getStatus());
            }

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

            log.info("分片上报成功: uploadId={}, chunkIndex={}, eTag={}", uploadId, chunkIndex, eTag);
        } catch (Exception e) {
            log.error("分片上报失败: {}", fileUploadChunkDTO, e);
            throw new BusinessException("分片上报失败: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UploadCompleteVO completeChunkUpload(FileUploadCompleteDTO fileUploadCompleteDTO) {
        try {
            log.info("开始分片合并: uploadId={}", fileUploadCompleteDTO.getUploadId());

            String uploadId = fileUploadCompleteDTO.getUploadId();
            Integer chunkTotalSize = fileUploadCompleteDTO.getChunkTotalSize();

            String chunkStatusKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
            String chunkETagKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
            String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

            // 1. 获取文件信息
            String idStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
            if (idStr == null) {
                throw new BusinessException("上传文件ID不存在");
            }
            Long id = Long.valueOf(idStr);
            String fileKey = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileKey");
            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");
            String extension = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "extension");
            if (fileKey == null) {
                throw new BusinessException("文件上传信息不完整");
            }
            List<PartETag> partETagList;

            // 2. 判断 ZSet 的元素数量与实际分片总数是否相同
            Long redisETagCount = stringRedisTemplate.opsForZSet().size(chunkETagKey);

            if (redisETagCount != null && redisETagCount.longValue() == chunkTotalSize) {
                // 尝试从 Redis 获取有序的 ETag 列表
                Set<ZSetOperations.TypedTuple<String>> tuples =
                        stringRedisTemplate.opsForZSet().rangeWithScores(chunkETagKey, 0, -1);

                if (tuples == null || tuples.isEmpty()) {
                    log.warn("Redis 中的分片 ETag 列表为空或不存在，回退到 OSS 查询");
                    partETagList = getPartETagList(uploadId, fileKey);
                } else {
                    // 成功获取到 tuples，尝试构建 PartETag 列表
                    partETagList = tuples.stream()
                            .map(tuple -> {
                                Double score = tuple.getScore();
                                if (score == null) {
                                    log.warn("分片 score 为 null，跳过该分片，ETag: {}", tuple.getValue());
                                    return null;
                                }
                                return new PartETag(score.intValue(), tuple.getValue());
                            })
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparing(PartETag::getPartNumber))
                            .collect(Collectors.toList());

                    // 如果转换后数量不对，说明 Redis 数据可能损坏，也回退
                    if (partETagList.size() != chunkTotalSize) {
                        log.warn("Redis 中解析出的 ETag 数量({}) 与预期总分片数({}) 不一致，回退到 OSS 查询",
                                partETagList.size(), chunkTotalSize);
                        partETagList = getPartETagList(uploadId, fileKey);
                    }
                }
            } else {
                // Redis 分片数不匹配，直接回退到 OSS
                log.info("Redis 中记录的分片数({}) 与预期总数({}) 不一致，回退到 OSS 查询",
                        redisETagCount, chunkTotalSize);
                partETagList = getPartETagList(uploadId, fileKey);
            }

            if (partETagList.size() != chunkTotalSize) {
                // 获取缺失的分片索引
                List<Integer> missingChunkIndexes = getMissingChunkIndexes(partETagList, chunkTotalSize);
                log.info("分片数量不一致，已上传: {}，需要: {}，缺失分片: {}",
                        partETagList.size(), chunkTotalSize, missingChunkIndexes);
                return new UploadCompleteVO(true, missingChunkIndexes, null);
            }

            // 3. 执行 OSS 分片合并
            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(ossConfig.getBucketName(),
                            fileKey, uploadId, partETagList);

            CompleteMultipartUploadResult result;
            try {
                result = ossClient.completeMultipartUpload(completeRequest);
            } catch (Exception e) {
                log.error("OSS 分片合并失败", e);
                fileUploadMapper.updateUploadStatus(id, UploadStatusEnum.UPLOAD_FAILED.getStatus());
                throw new BusinessException("OSS 分片合并失败: " + e.getMessage());
            }

            // 获取上传成功后的阿里云OSS文件地址
            String location = result.getLocation();

            // 4. 更新数据库记录，更新文件OSS路径和上传状态为上传完成
            fileUploadMapper.updateFileLocationAndStatus(id, location, UploadStatusEnum.COMPLETED.getStatus());

            // 5. 删除 Redis 分片相关的缓存（批量删除）
            List<String> keysToDelete = new ArrayList<>();
            for (int i = 1; i <= chunkTotalSize; i++) {
                keysToDelete.add(String.format(UploadRedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, i));
            }
            keysToDelete.add(chunkStatusKey);
            keysToDelete.add(chunkETagKey);
            stringRedisTemplate.delete(keysToDelete);

            log.info("合并完成，文件名：{}，uploadId：{}，文件地址：{}", fileName, uploadId, location);

            // 6. 合并完成后发送 mq 消息根据 location 地址下载文件并进行文件的向量化处理
            String userIdStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "userId");
            if (userIdStr == null) {
                throw new IllegalArgumentException("userId 不存在");
            }
            Long userId = Long.valueOf(userIdStr.trim());

            String knowledgeBaseIdStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "knowledgeBaseId");
            if (knowledgeBaseIdStr == null) {
                throw new IllegalArgumentException("knowledgeBaseId 不存在");
            }
            Long knowledgeBaseId = Long.valueOf(knowledgeBaseIdStr.trim());

            String organizationIdStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "organizationId");
            if (organizationIdStr == null) {
                throw new IllegalArgumentException("organizationId 不存在");
            }
            Long organizationId = Long.valueOf(organizationIdStr.trim());

            VectorBO vectorBO = VectorBO.builder()
                    .organizationId(organizationId)
                    .fileId(id)
                    .knowledgeBaseId(knowledgeBaseId)
                    .userId(userId)
                    .build();

            FileVectorizeEvent.FileVectorizeMessage fileVectorizeMessage =  FileVectorizeEvent.FileVectorizeMessage
                    .builder()
                    .vectorBO(vectorBO)
                    .location(location)
                    .fileName(fileName)
                    .extension(extension)
                    .build();
            eventPublisher.publish(fileVectorizeEvent.topic(), fileVectorizeEvent.buildEventMessage(fileVectorizeMessage));
            return new UploadCompleteVO(false, null, location);
        } catch (Exception e) {
            log.error("分片合并失败", e);
            throw new BusinessException("分片合并失败: " + e.getMessage());
        }
    }

    @Override
    public UploadProgressVO getUploadProgress(String uploadId, String fileKey) {
        String chunkStatusKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
        String chunkETagKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);

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
                throw e;
            }
        }

        return new UploadProgressVO(uploadedParts);
    }

    @Override
    public void cancelUpload(FileUploadCancelDTO fileUploadCancelDTO) {
        String uploadId = fileUploadCancelDTO.getUploadId();
        log.info("开始取消上传，uploadId: {}", uploadId);

        String chunkStatusKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_STATUS_KEY, uploadId);
        String chunkETagKey = String.format(UploadRedisKeyConstant.FILE_CHUNK_ETAG_KEY, uploadId);
        String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

        try {
            // 从Redis获取文件上传信息
            String idStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
            if (idStr == null) {
                throw new BusinessException("上传文件ID不存在");
            }
            Long id = Long.valueOf(idStr);
            String fileKey = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileKey");
            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");
            String chunkTotal = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "chunkTotal");

            if (fileKey == null) {
                log.warn("文件上传信息不存在或已过期，uploadId: {}", uploadId);
                throw new BusinessException("上传任务不存在或已过期");
            }

            // 1. 终止阿里云OSS的分片上传
            try {
                AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                        ossConfig.getBucketName(), fileKey, uploadId);
                ossClient.abortMultipartUpload(abortRequest);
                log.info("OSS分片上传已终止，uploadId: {}", uploadId);
            } catch (OSSException e) {
                if (!"NoSuchUpload".equals(e.getErrorCode())) {
                    log.error("终止OSS分片上传失败，uploadId: {}, errorCode: {}, message: {}",
                            uploadId, e.getErrorCode(), e.getMessage());
                } else {
                    log.info("OSS分片上传不存在或已过期，uploadId: {}", uploadId);
                }
            } catch (Exception e) {
                log.error("终止OSS分片上传时发生异常，uploadId: {}", uploadId, e);
            }

            // 2. 更新数据库状态为取消上传
            fileUploadMapper.updateUploadStatus(id, UploadStatusEnum.CANCELED.getStatus());
            log.info("文件上传状态已更新为取消，fileId: {}, fileName: {}", id, fileName);

            // 3. 清理Redis中的相关数据
            List<String> keysToDelete = new ArrayList<>();
            if (chunkTotal != null) {
                int totalChunks = Integer.parseInt(chunkTotal);
                for (int i = 1; i <= totalChunks; i++) {
                    keysToDelete.add(String.format(UploadRedisKeyConstant.FILE_CHUNK_INFO_KEY, uploadId, i));
                }
            }
            keysToDelete.add(chunkStatusKey);
            keysToDelete.add(chunkETagKey);
            keysToDelete.add(fileUploadInfoKey);

            stringRedisTemplate.delete(keysToDelete);
            log.info("Redis缓存已清理，uploadId: {}", uploadId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消上传失败，uploadId: {}", uploadId, e);
            throw new BusinessException("取消上传失败: " + e.getMessage());
        }
    }

    @Override
    public void pauseUpload(FileUploadPauseDTO fileUploadPauseDTO) {
        String uploadId = fileUploadPauseDTO.getUploadId();
        log.info("开始暂停上传，uploadId: {}", uploadId);

        String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

        try {
            // 从Redis获取文件信息
            String idStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
            if (idStr == null) {
                throw new BusinessException("上传文件ID不存在");
            }
            Long id = Long.valueOf(idStr);
            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");

            // 更新数据库状态为暂停上传
            fileUploadMapper.updateUploadStatus(id, UploadStatusEnum.PAUSED.getStatus());
            log.info("文件上传状态已更新为暂停，fileId: {}, fileName: {}", id, fileName);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("暂停上传失败，uploadId: {}", uploadId, e);
            throw new BusinessException("暂停上传失败: " + e.getMessage());
        }
    }

    @Override
    public UploadInitVO resumeUpload(FileUploadPauseDTO fileUploadPauseDTO) {
        String uploadId = fileUploadPauseDTO.getUploadId();
        log.info("开始恢复上传，uploadId: {}", uploadId);

        String fileUploadInfoKey = String.format(UploadRedisKeyConstant.FILE_UPLOAD_INIT_KEY, uploadId);

        try {
            // 从Redis获取文件上传信息
            String idStr = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "id");
            if (idStr == null) {
                throw new BusinessException("上传文件ID不存在");
            }
            Long id = Long.valueOf(idStr);
            String fileName = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileName");
            String fileKey = (String) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "fileKey");
            Integer chunkTotal = (Integer) stringRedisTemplate.opsForHash().get(fileUploadInfoKey, "chunkTotal");

            if (fileKey == null || chunkTotal == null) {
                log.warn("文件上传信息不完整或已过期，uploadId: {}", uploadId);
                throw new BusinessException("上传任务信息不完整或已过期");
            }

            // 检查当前文件状态是否允许恢复
            FileUpload fileUpload = fileUploadMapper.selectById(id);
            if (fileUpload == null) {
                throw new BusinessException("文件记录不存在");
            }

            if (!fileUpload.getStatus().equals(UploadStatusEnum.PAUSED.getStatus()) &&
                !fileUpload.getStatus().equals(UploadStatusEnum.UPLOADING.getStatus()) &&
                !fileUpload.getStatus().equals(UploadStatusEnum.INITIALIZED.getStatus())) {
                throw new BusinessException("当前文件状态不允许恢复上传");
            }

            // 检查OSS上传会话是否仍然有效
            Map<Integer, String> uploadUrls = new HashMap<>();
            long expireSeconds = ossConfig.getPresignedUrlExpiration();
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);

            try {
                // 验证uploadId是否仍然有效
                ListPartsRequest listPartsRequest = new ListPartsRequest(ossConfig.getBucketName(), fileKey, uploadId);
                ossClient.listParts(listPartsRequest);

                // 如果uploadId有效，重新生成预签名URL
                for (int i = 1; i <= chunkTotal; i++) {
                    GeneratePresignedUrlRequest urlRequest =
                            new GeneratePresignedUrlRequest(ossConfig.getBucketName(), fileKey, HttpMethod.PUT);
                    urlRequest.setExpiration(expiration);

                    Map<String, String> queryParams = new HashMap<>(2);
                    queryParams.put("uploadId", uploadId);
                    queryParams.put("partNumber", String.valueOf(i));
                    urlRequest.setQueryParameter(queryParams);

                    URL signedUrl = ossClient.generatePresignedUrl(urlRequest);
                    uploadUrls.put(i, signedUrl.toString());
                }

                // 更新数据库状态为上传中
                fileUploadMapper.updateUploadStatus(id, UploadStatusEnum.UPLOADING.getStatus());
                log.info("文件上传状态已更新为上传中，fileId: {}, fileName: {}", id, fileName);

                // 更新Redis过期时间
                stringRedisTemplate.expire(fileUploadInfoKey, expireSeconds, TimeUnit.SECONDS);

                return new UploadInitVO(UploadStatusEnum.UPLOADING.getStatus(), uploadId, null, uploadUrls);

            } catch (OSSException e) {
                if ("NoSuchUpload".equals(e.getErrorCode())) {
                    log.warn("OSS上传会话已过期，需要重新初始化，uploadId: {}", uploadId);
                    throw new BusinessException("上传会话已过期，请重新初始化上传");
                } else {
                    log.error("检查OSS上传会话时发生错误，uploadId: {}, errorCode: {}, message: {}",
                            uploadId, e.getErrorCode(), e.getMessage());
                    throw new BusinessException("恢复上传失败: " + e.getMessage());
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("恢复上传失败，uploadId: {}", uploadId, e);
            throw new BusinessException("恢复上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取缺失的分片ETag索引列表
     */
    private List<Integer> getMissingChunkIndexes(List<PartETag> uploadedParts, int totalChunks) {
        // 获取已上传的分片编号集合
        Set<Integer> uploadedPartNumbers = uploadedParts.stream()
                .map(PartETag::getPartNumber)
                .collect(Collectors.toSet());

        // 找出缺失的分片索引
        List<Integer> missingIndexes = new ArrayList<>();
        for (int i = 1; i <= totalChunks; i++) {
            if (!uploadedPartNumbers.contains(i)) {
                missingIndexes.add(i);
            }
        }
        return missingIndexes;
    }

    /**
     *  从阿里云OSS获取分片ETag列表
     */
    public List<PartETag> getPartETagList(String uploadId, String fileKey) {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            log.warn("getPartETagList 调用失败：uploadId 不能为空");
            throw new IllegalArgumentException("uploadId 不能为空");
        }
        if (fileKey == null || fileKey.trim().isEmpty()) {
            log.warn("getPartETagList 调用失败：fileKey 不能为空");
            throw new IllegalArgumentException("fileKey 不能为空");
        }

        try {
            // 调用阿里云 OSS 的 ListParts 接口
            ListPartsRequest listPartsRequest = new ListPartsRequest(
                    ossConfig.getBucketName(),
                    fileKey,
                    uploadId
            );

            PartListing partListing = ossClient.listParts(listPartsRequest);
            // TODO：更新 Redis 的分片ETag列表

            // 将 Part 列表转换为 PartETag 列表
            return partListing.getParts().stream()
                    .map(part -> new PartETag(part.getPartNumber(), part.getETag()))
                    .collect(Collectors.toList());
        } catch (OSSException e) {
            // OSS 服务端异常：如 NoSuchUpload, AccessDenied, NoSuchBucket 等
            log.error("OSS服务端错误 - ListParts 失败。Bucket: {}, fileKey: {}, uploadId: {}, ErrorCode: {}, Message: {}, RequestId: {}",
                    ossConfig.getBucketName(), fileKey, uploadId, e.getErrorCode(), e.getMessage(), e.getRequestId(), e);
            throw new RuntimeException("OSS 错误: " + e.getMessage(), e);
        } catch (ClientException e) {
            // 客户端异常：网络问题、SDK内部错误等
            log.error("客户端错误 - 调用 ListParts 失败。Bucket: {}, fileKey: {}, uploadId: {}, Message: {}",
                    ossConfig.getBucketName(), fileKey, uploadId, e.getMessage(), e);
            throw new RuntimeException("网络或客户端错误，无法获取分片列表", e);
        }
    }
}