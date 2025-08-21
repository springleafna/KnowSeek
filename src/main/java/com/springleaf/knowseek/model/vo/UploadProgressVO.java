package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 上传进度VO
 */
@Data
@AllArgsConstructor
public class UploadProgressVO {

    /**
     * 已上传的分片编号列表
     */
    List<Integer> uploadedParts;
}
