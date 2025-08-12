package com.springleaf.knowseek.constans;

public class RedisKeyConstant {

    /**
     * 文件上传信息 HASH：upload_file_info_{uploadId}
     */
    public static String FILE_UPLOAD_INIT_KEY = "upload_file_info_%s";

    /**
     * 分片信息 HASH：upload_chunk_info_{uploadId}_{index}
     */
    public static String FILE_CHUNK_INFO_KEY = "upload_chunk_info_%s_%s";

    /**
     * 分片状态 BitMap：upload_chunk_status_{uploadId}
     */
    public static String FILE_CHUNK_STATUS_KEY = "upload_chunk_status_%s";

    /**
     * 分片 ETag 有序列表 List：upload_chunk_eTag_{uploadId}
     */
    public static String FILE_CHUNK_ETAG_KEY = "upload_chunk_eTag_%s";
}
