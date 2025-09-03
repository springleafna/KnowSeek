package com.springleaf.knowseek.constans;

/**
 * 文件上传相关 Redis Key
 */
public class UploadRedisKeyConstant {

    /**
     * 文件上传信息 HASH：upload_file_info_{uploadId}
     */
    public static final String FILE_UPLOAD_INIT_KEY = "knowseek:upload_file:info:%s";

    /**
     * 分片信息 HASH：upload_chunk_info_{uploadId}_{index}
     */
    public static final String FILE_CHUNK_INFO_KEY = "knowseek:upload_chunk:info:%s_%s";

    /**
     * 分片状态 BitMap：upload_chunk_status_{uploadId}
     */
    public static final String FILE_CHUNK_STATUS_KEY = "knowseek:upload_chunk:status:%s";

    /**
     * 分片 ETag 有序集合 ZSet：upload_chunk_eTag_{uploadId}
     */
    public static final String FILE_CHUNK_ETAG_KEY = "knowseek:upload_chunk:eTag:%s";

}
