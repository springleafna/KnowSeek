package com.springleaf.knowseek.mapper;

import com.springleaf.knowseek.model.entity.FileUpload;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileUploadMapper {

    /**
     * 新增文件上传
     */
    @Insert("INSET INTO tb_tb_pai_smart")
    int saveFileUpload(FileUpload fileUpload);

    /**
     * 判断是否该文件是否已经被上传成功
     */
    @Select("SELECT * FROM tb_pai_smart WHERE file_md5 = #{fileMd5} AND user_id = #{userId} AND status = #{status}")
    boolean existFileUpload(@Param("fileMd5") String fileMd5, @Param("userId") Long userId, @Param("status") int status);

    int updateOSSLocation(String location);

    int updateUploadStatus(int status);
}
