CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE tb_vector_record (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    organization_id BIGINT,
    file_id BIGINT NOT NULL,
    embedding VECTOR(1024) NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- 确保同一文件内分片索引唯一
    UNIQUE (file_id, chunk_index)
);

-- 创建索引加速查询
CREATE INDEX idx_tb_vector_record_file_id ON tb_vector_record (file_id);
CREATE INDEX idx_tb_vector_record_file_chunk ON tb_vector_record (file_id, chunk_index);
CREATE INDEX idx_tb_vector_record_knowledge_base ON tb_vector_record (knowledge_base_id);
CREATE INDEX idx_tb_vector_record_user ON tb_vector_record (user_id);

-- 如果要做向量相似度搜索，强烈建议加 IVFFLAT 或 HNSW 索引（pgvector 支持）
-- 方式 1: HNSW (推荐，速度快，精度高)
CREATE INDEX idx_tb_vector_record_embedding
    ON tb_vector_record USING hnsw (embedding vector_cosine_ops);

-- 或者 方式 2: IVFFlat (内存占用少，适合数据量巨大但精度要求稍低的场景)
-- CREATE INDEX idx_tb_vector_record_embedding
-- ON tb_vector_record USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);