package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.dto.FileUploadCompleteDTO;
import com.springleaf.knowseek.model.vo.UploadCompleteVO;
import com.springleaf.knowseek.model.vo.UploadInitVO;
import com.springleaf.knowseek.model.vo.UploadProgressVO;

import java.io.IOException;

public interface FileService {


    /**
     * 分片文件上传
     */
    String uploadChunk(FileUploadChunkDTO fileUploadChunkDTO);

    /**
     * 文件上传初始化
     */
    UploadInitVO initFileUpload(FileUploadChunkInitDTO fileUploadChunkInitDTO);

    /**
     * 完成分片上传，进行合并
     */
    UploadCompleteVO completeChunkUpload(FileUploadCompleteDTO fileUploadCompleteDTO);

    /**
     * 获取上传进度
     */
    UploadProgressVO getUploadProgress(String uploadId, String fileKey);
}
