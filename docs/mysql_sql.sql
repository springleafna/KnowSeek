CREATE DATABASE know_seek if not exists;

CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名，唯一',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    primary_org_id BIGINT DEFAULT NULL COMMENT '用户主组织ID',
    primary_knowledge_base_id BIGINT DEFAULT NULL COMMENT '用户主知识库ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE tb_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识库ID',
    name VARCHAR(255) NOT NULL COMMENT '知识库名称',
    user_id BIGINT NOT NULL COMMENT '用户id',
    description TEXT DEFAULT NULL COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

CREATE TABLE tb_file_upload (
    id           BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    file_md5     VARCHAR(32)      NOT NULL COMMENT '文件 MD5',
    file_name    VARCHAR(255)     NOT NULL COMMENT '文件名称',
    total_size   BIGINT           NOT NULL COMMENT '文件大小',
    status       TINYINT          NOT NULL DEFAULT 0 COMMENT '上传状态，0-已上传，1-初始化完成，2-上传中，3-暂停上传，4-取消上传，5-上传失败，6-处理中，7-处理失败，8-处理完成',
    user_id      BIGINT           NOT NULL COMMENT '用户 ID',
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库ID',
    org_tag      VARCHAR(50)      DEFAULT NULL COMMENT '组织标签',
    is_public    BOOLEAN          NOT NULL DEFAULT FALSE COMMENT '是否公开',
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    location 		 VARCHAR(255)     DEFAULT NULL COMMENT '阿里云OSS地址',
    merged_at    TIMESTAMP        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '合并时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_md5_user (file_md5, user_id, knowledge_base_id),
    INDEX idx_user (user_id),
    INDEX idx_org_tag (org_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';

CREATE TABLE tb_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '组织ID',
    tag VARCHAR(50) UNIQUE NOT NULL COMMENT '组织唯一标签名',
    name VARCHAR(100) NOT NULL COMMENT '组织名称',
    description TEXT DEFAULT NULL COMMENT '描述',
    parent_id BIGINT DEFAULT NULL COMMENT '父组织ID',
    created_by BIGINT NOT NULL COMMENT '创建者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

CREATE TABLE tb_user_organization (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    organization_id BIGINT NOT NULL COMMENT '组织ID',

    PRIMARY KEY (user_id, organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组织关联表';

CREATE TABLE tb_vector_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    file_md5 CHAR(32) NOT NULL COMMENT '文件 MD5（文件指纹）',
    chunk_id INT NOT NULL COMMENT '文本分块序号',
    text_content LONGTEXT COMMENT '原始文本内容（压缩存储）',
    model_version VARCHAR(32) DEFAULT 'text-embedding-v4' COMMENT '向量模型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量分片表';

CREATE TABLE tb_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话唯一ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    session_name VARCHAR(255) DEFAULT '新对话' COMMENT '会话名称，便于用户识别',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃（可选）',
    metadata JSON DEFAULT NULL COMMENT '扩展字段，如模型版本、温度等配置'
) COMMENT='AI对话会话，每个会话独立上下文';

CREATE TABLE tb_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息唯一ID',
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    role ENUM('user', 'assistant', 'system') NOT NULL COMMENT '消息角色：用户/助手/系统',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    metadata JSON DEFAULT NULL COMMENT '扩展字段，如模型、耗时、插件调用等'
) COMMENT='AI对话消息记录，按会话隔离';

CREATE TABLE tb_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    ip_address VARCHAR(45) NOT NULL COMMENT '操作IP地址',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型（如：新增、修改、删除等）',
    description VARCHAR(255) NOT NULL COMMENT '操作描述',
    request_url VARCHAR(255) NOT NULL COMMENT '请求URL',
    request_method VARCHAR(10) NOT NULL COMMENT '请求方法（GET/POST/PUT/DELETE等）',
    request_params TEXT COMMENT '请求参数（JSON格式）',
    response_result VARCHAR(20) NOT NULL COMMENT '响应结果（如：success/fail）',
    response_message VARCHAR(500) COMMENT '响应消息',
    operation_time DATETIME(3) NOT NULL COMMENT '操作时间（精确到毫秒）',
    execution_time INT UNSIGNED NOT NULL COMMENT '执行耗时（单位：毫秒）',

    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';