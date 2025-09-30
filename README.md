# KnowSeek - 知识检索系统

## 项目简介

KnowSeek 是一个基于 AI 的知识库管理与智能问答系统,集成了向量检索、文件解析、智能对话等功能。该系统支持多种文档格式的知识提取,通过向量化技术实现精准的语义检索,并结合大语言模型提供智能问答服务。

## 核心功能

- **知识库管理**: 支持创建、更新、删除知识库,以及设置用户主知识库
- **文件管理**: 支持多种文档格式上传(支持大文件分片上传),自动解析并向量化存储
- **智能对话**: 基于知识库的 AI 问答,支持知识溯源与多文件独立标注
- **向量检索**: 基于距离评分的增强型向量检索,支持按文件相关性排序
- **用户与组织**: 完整的用户认证、角色管理和组织架构体系
- **会话管理**: 支持创建、更新、删除对话会话,管理会话消息记录

## 技术栈

### 后端框架
- **Spring Boot 3.4.2**: 核心应用框架
- **Java 17**: 开发语言

### 数据存储
- **MySQL**: 主数据库,存储业务数据
- **PostgreSQL + pgvector**: 向量数据库,存储文档向量
- **Redis**: 缓存与会话管理
- **Elasticsearch**: 全文检索引擎

### AI 能力
- **Spring AI**: AI 框架集成
- **阿里云通义千问**:
  - `qwen-plus`: 聊天模型
  - `text-embedding-v2`: 文本嵌入模型(1024维)

### 消息队列
- **RabbitMQ**: 异步文件向量化处理,支持死信队列机制

### 文件处理
- **Apache Tika**: 文档解析(支持 PDF、Word、TXT 等多种格式)
- **阿里云 OSS**: 对象存储服务

### 安全与认证
- **Sa-Token**: 权限认证框架
- **提示词安全防护**: 多层检测与防御机制

### 其他依赖
- **MyBatis**: ORM 框架
- **PageHelper**: 分页插件
- **Lombok**: 简化 Java 代码
- **FastJSON2**: JSON 处理
- **Commons IO**: 文件工具库

## 项目结构

```
KnowSeek/
├── src/main/java/com/springleaf/knowseek/
│   ├── config/                 # 配置类(数据源、Sa-Token、OSS、RabbitMQ等)
│   ├── controller/             # 控制层
│   │   ├── ChatController.java           # 对话接口
│   │   ├── FileController.java           # 文件上传接口
│   │   ├── KnowledgeBaseController.java  # 知识库管理接口
│   │   ├── OrganizationController.java   # 组织管理接口
│   │   └── UserController.java           # 用户管理接口
│   ├── service/                # 服务层
│   │   ├── impl/               # 服务实现
│   │   ├── ChatService.java           # 对话服务
│   │   ├── EmbeddingService.java       # 向量嵌入服务
│   │   ├── KnowledgeBaseService.java   # 知识库服务
│   │   ├── MessageService.java         # 消息服务
│   │   ├── SessionService.java         # 会话服务
│   │   ├── VectorRecordService.java    # 向量记录服务
│   │   └── ...
│   ├── mapper/                 # 数据访问层
│   │   ├── mysql/              # MySQL Mapper
│   │   └── pgvector/           # PostgreSQL Mapper
│   ├── model/                  # 数据模型
│   │   ├── entity/             # 实体类
│   │   ├── dto/                # 数据传输对象
│   │   ├── vo/                 # 视图对象
│   │   └── bo/                 # 业务对象
│   ├── mq/                     # 消息队列
│   │   ├── producer/           # 消息发布者
│   │   ├── consumer/           # 消息消费者
│   │   └── event/              # 事件定义
│   ├── utils/                  # 工具类
│   ├── enums/                  # 枚举类
│   ├── constants/              # 常量定义
│   ├── exception/              # 自定义异常
│   ├── handler/                # 处理器(全局异常、类型转换等)
│   └── common/                 # 公共类
├── src/main/resources/
│   ├── mapper/                 # MyBatis XML 映射文件
│   └── application.yml         # 应用配置文件
├── docs/                       # 文档目录
│   ├── mysql_sql.sql           # MySQL 数据库脚本
│   ├── postgres_sql.sql        # PostgreSQL 数据库脚本
│   └── prompt.txt              # 提示词模板
└── pom.xml                     # Maven 配置文件
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- PostgreSQL 12+ (with pgvector extension)
- Redis 6.0+
- RabbitMQ 3.8+
- Elasticsearch 8.x

### 配置说明

1. **数据库配置**

复制 `src/main/resources/application.yml` 并修改以下配置:

```yaml
spring:
  datasource:
    mysql:
      url: jdbc:mysql://your-host:3306/know_seek
      username: your_username
      password: your_password
    pgvector:
      url: jdbc:postgresql://your-host:5432/know_seek
      username: your_username
      password: your_password
```

执行数据库初始化脚本:
- MySQL: `docs/mysql_sql.sql`
- PostgreSQL: `docs/postgres_sql.sql` (需先安装 pgvector 扩展)

2. **Redis 配置**

```yaml
spring:
  data:
    redis:
      host: your-redis-host
      port: 6379
      password: your_password
```

3. **RabbitMQ 配置**

```yaml
spring:
  rabbitmq:
    host: your-rabbitmq-host
    port: 5672
    username: your_username
    password: your_password
```

4. **Elasticsearch 配置**

```yaml
spring:
  elasticsearch:
    uris: your-elasticsearch-host:9200
```

5. **阿里云服务配置**

```yaml
# 通义千问 API
spring:
  ai:
    openai:
      api-key: your_dashscope_api_key

# OSS 对象存储
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    bucket-name: your_bucket_name
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
```

### 启动应用

1. **编译项目**

```bash
mvn clean package
```

2. **运行应用**

```bash
java -jar target/KnowSeek-1.0-SNAPSHOT.jar
```

或直接运行主类:
```bash
mvn spring-boot:run
```

3. **访问应用**

默认端口: `8181`

API 基础路径: `http://localhost:8181`

## API 接口

### 用户管理
- `POST /user/register` - 用户注册
- `POST /user/login` - 用户登录
- `GET /user/info` - 获取用户信息
- `GET /user/page` - 分页查询用户列表

### 知识库管理
- `POST /knowledge-base` - 创建知识库
- `PUT /knowledge-base` - 更新知识库
- `DELETE /knowledge-base/{id}` - 删除知识库
- `GET /knowledge-base/{id}` - 获取知识库详情
- `POST /knowledge-base/set-main` - 设置主知识库

### 文件管理
- `POST /file/upload/init` - 初始化分片上传
- `POST /file/upload/chunk` - 上传文件分片
- `POST /file/upload/complete` - 完成文件上传
- `POST /file/upload/pause` - 暂停上传
- `POST /file/upload/cancel` - 取消上传
- `GET /file/upload/progress/{uploadId}` - 查询上传进度
- `GET /file/page` - 分页查询文件列表

### 对话管理
- `POST /chat/session` - 创建会话
- `PUT /chat/session` - 更新会话
- `DELETE /chat/session/{id}` - 删除会话
- `GET /chat/session/list` - 获取会话列表
- `POST /chat/message` - 发送消息
- `GET /chat/message/{sessionId}` - 获取会话消息记录

### 组织管理
- `POST /organization` - 创建组织
- `PUT /organization` - 更新组织
- `DELETE /organization/{id}` - 删除组织
- `GET /organization/tree` - 获取组织树
- `POST /organization/assign` - 分配用户到组织

## 核心流程

### 文件上传与向量化流程

1. 客户端初始化分片上传 → 获取 `uploadId` 和预签名 URL
2. 客户端直传分片到 OSS
3. 完成上传后触发文件向量化事件
4. RabbitMQ 消费者异步处理:
   - Apache Tika 解析文件内容
   - 文本分块处理
   - 调用通义千问 embedding 接口生成向量
   - 存储向量到 PostgreSQL pgvector
5. 更新文件状态为"已完成"

### 智能问答流程

1. 用户发送问题
2. 提示词安全检测(防止注入攻击)
3. 问题向量化
4. 向量检索相关知识片段(基于余弦相似度)
5. 构建增强提示词(问题 + 知识上下文)
6. 调用通义千问生成回答
7. 知识溯源标注(标记回答来源的文件)
8. 返回结果并存储对话记录

## 主要特性

### 1. 增强型向量检索
- 支持基于距离评分的相关性排序
- 自动过滤低相关度结果
- 优化知识上下文构建,提升回答准确性

### 2. 安全防护机制
- 多层提示词注入检测
- 防御性系统提示构建
- 输入/输出安全检测与拦截

### 3. 大文件支持
- 分片上传机制
- 断点续传支持
- 上传进度实时查询
- 暂停/取消上传功能

### 4. 异步处理
- RabbitMQ 消息队列解耦
- 死信队列处理失败任务
- 支持重试机制(最多3次)

### 5. 多租户架构
- 组织层级管理
- 用户角色权限控制
- 知识库隔离

## 数据库设计

### MySQL (业务数据)
- `user` - 用户表
- `organization` - 组织表
- `user_organization` - 用户组织关系表
- `knowledge_base` - 知识库表
- `session` - 会话表
- `message` - 消息表

### PostgreSQL + pgvector (向量数据)
- `vector_record` - 向量记录表
  - `id`: 主键
  - `file_id`: 文件ID
  - `content`: 文本内容
  - `embedding`: 向量(vector类型)
  - `knowledge_base_id`: 知识库ID

## 监控与日志

应用使用 SLF4J + Logback 进行日志管理,支持以下日志级别配置:

```yaml
logging:
  level:
    com.springleaf.knowseek.mapper: DEBUG
    org.apache.ibatis: DEBUG
    org.springframework.ai: DEBUG
```

## 性能优化

- 数据库连接池优化(HikariCP)
- Redis 连接池配置
- 空闲连接自动回收机制
- 分页查询支持
- 向量检索结果缓存

## 注意事项

1. **pgvector 扩展**: PostgreSQL 需要先安装 pgvector 扩展才能使用向量检索功能
   ```sql
   CREATE EXTENSION vector;
   ```

2. **API Key 安全**: 生产环境建议使用环境变量管理敏感配置,不要将密钥硬编码在配置文件中

3. **向量维度**: 当前使用 text-embedding-v2 模型,向量维度为 1024,切换模型需同步修改数据库表结构

4. **消息队列超时**: 大文件处理超时时间已延长至 10 分钟,超大文件可能需要进一步调整

## 开发计划

- [ ] 增加知识库统计分析功能
- [ ] 支持多模态检索(图片、音频)
- [ ] 实现知识图谱可视化
- [ ] 添加完整的单元测试

## 许可证

本项目采用 MIT 许可证,详见 LICENSE 文件。

## 联系方式

如有问题或建议,请提交 Issue 或 Pull Request。

---

**KnowSeek** - 让知识触手可及