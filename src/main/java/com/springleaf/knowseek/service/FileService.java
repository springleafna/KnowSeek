package com.springleaf.knowseek.service;

import com.springleaf.knowseek.model.dto.FileUploadChunkDTO;

public interface FileService {


    void uploadChunk(FileUploadChunkDTO fileUploadChunkDTO);
}
