package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;
import com.springleaf.knowseek.model.dto.FileUploadChunkInitDTO;
import com.springleaf.knowseek.model.vo.UploadInitVO;

public interface FileService {


    void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO);

    /**
     * 文件上传初始化
     */
    UploadInitVO initFileUpload(FileUploadChunkInitDTO fileUploadChunkInitDTO);
}
