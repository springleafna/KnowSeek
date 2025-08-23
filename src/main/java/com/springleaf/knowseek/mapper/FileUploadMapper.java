package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.FileUpload;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FileUploadMapper {

    /**
     * 新增文件上传记录
     */
    int saveFileUpload(FileUpload fileUpload);

    /**
     * 判断文件是否已上传成功
     */
    FileUpload existFileUpload(@Param("fileMd5") String fileMd5,
                            @Param("userId") Long userId);

    /**
     * 更新文件的 OSS 地址
     */
    int updateOSSLocation(@Param("id") Long id, @Param("location") String location);

    /**
     * 更新文件上传状态
     */
    int updateUploadStatus(@Param("id") Long id, @Param("status") int status);

    /**
     * 根据 MD5 和用户 ID 查询文件详情
     */
    FileUpload findByMd5AndUserId(@Param("fileMd5") String fileMd5, @Param("userId") Long userId);
}

