package com.springleaf.knowseek.service;

import com.github.pagehelper.PageInfo;
import com.springleaf.knowseek.model.dto.*;
import com.springleaf.knowseek.model.vo.*;

import java.util.List;

public interface FileService {

    /**
     * 获取文件列表
     */
    PageInfo<FileItemVO> getFileList(FilePageDTO filePageDTO);

    /**
     * 文件上传初始化
     */
    UploadInitVO initFileUpload(FileUploadChunkInitDTO fileUploadChunkInitDTO);

    /**
     * 保存分片信息
     */
    void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO);

    /**
     * 完成分片上传，进行合并
     */
    UploadCompleteVO completeChunkUpload(FileUploadCompleteDTO fileUploadCompleteDTO);

    /**
     * 获取上传进度
     */
    UploadProgressVO getUploadProgress(String uploadId, String fileKey);

    /**
     * 取消上传
     */
    void cancelUpload(FileUploadCancelDTO fileUploadCancelDTO);

    /**
     * 暂停上传
     */
    void pauseUpload(FileUploadPauseDTO fileUploadPauseDTO);

    /**
     * 恢复上传
     */
    UploadInitVO resumeUpload(FileUploadPauseDTO fileUploadPauseDTO);

    /**
     * 删除文件
     */
    void deleteFile(Long id);

    /**
     * 文件下载
     */
    String downloadFile(Long id);

    /**
     * 获取文件分片列表详情
     */
    List<FileChunkDetailVO> getFileDetail(Long fileId);
}
