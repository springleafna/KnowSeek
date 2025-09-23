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
CREATE INDEX ON tb_vector_record
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);  -- 根据数据量调整 lists，一般为数据量的 1/10 ~ 1/100

-- 或者用 HNSW（PostgreSQL 17+ 或 pgvector 0.5+）
-- CREATE INDEX ON tb_vector_record
-- USING hnsw (embedding vector_cosine_ops);