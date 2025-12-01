package com.springleaf.knowseek.constans;

/**
 * Rag检索相关常量
 */
public final class RagConstant {

    /**
     * 检索时最多召回的文档数量
     */
    public static final int TOPK = 15;

    /**
     * 召回后最多保留的文件数
     */
    public static final int MAX_FILES = 3;

    /**
     * 每个文件最多保留的分片数
     */
    public static final int CHUNKS_PER_FILE = 3;
}
