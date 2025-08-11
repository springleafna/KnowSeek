package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.FileUpload;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileUploadMapper {

    /**
     * 新增文件上传
     */
    int saveFileUpload(FileUpload fileUpload);

    /**
     * 判断是否该文件是否已经被上传成功
     */
    boolean existFileUpload(String fileMd5, Long userId, int status);
}
