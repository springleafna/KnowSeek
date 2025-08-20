CREATE DATABASE know_seek if not exists;

CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户唯一标识',
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名，唯一',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    primary_org_id BIGINT DEFAULT NULL COMMENT '用户主组织ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE file_upload (
    id           BIGINT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_md5     VARCHAR(32)      NOT NULL COMMENT '文件 MD5',
    file_name    VARCHAR(255)     NOT NULL COMMENT '文件名称',
    total_size   BIGINT           NOT NULL COMMENT '文件大小',
    status       TINYINT          NOT NULL DEFAULT 0 COMMENT '上传状态，0-已上传，1-上传中，2-上传失败',
    user_id      BIGINT           NOT NULL COMMENT '用户 ID',
    org_tag      VARCHAR(50)      DEFAULT NULL COMMENT '组织标签',
    is_public    BOOLEAN          NOT NULL DEFAULT FALSE COMMENT '是否公开',
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    location 		 VARCHAR(255)     DEFAULT NULL COMMENT '阿里云OSS地址',
    merged_at    TIMESTAMP        NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '合并时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_md5_user (file_md5, user_id),
    INDEX idx_user (user_id),
    INDEX idx_org_tag (org_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录';

CREATE TABLE tb_organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '组织唯一标识',
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
    user_id BIGINT NOT NULL COMMENT '用户id',
    organization_id BIGINT NOT NULL COMMENT '组织id',

    PRIMARY KEY (user_id, organization_id)
);