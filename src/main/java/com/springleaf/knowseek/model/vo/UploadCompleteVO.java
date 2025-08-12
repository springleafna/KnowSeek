package com.springleaf.knowseek.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分片合并VO
 */
@Data
@AllArgsConstructor
public class UploadCompleteVO {

    /**
     * 是否重新上传分片（存在未成功上传的分片时未true）
     */
    private Boolean reUpload;

    /**
     * 未上传的分片索引集合，需要重新上传
     */
    private List<Integer> pendingChunkIndexList;

    /**
     * 上传至阿里云OSS的文件地址
     */
    private String location;
}
