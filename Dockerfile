# 使用 OpenJDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建后的 jar 文件到容器中
COPY target/KnowSeek-1.0-SNAPSHOT.jar app.jar

# 暴露 Spring Boot 默认端口
EXPOSE 8181

# 设置 JVM 参数（可选，根据需要调整）
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]