# 使用主流维护的 OpenJDK 21 运行时镜像 (JRE 比 JDK 更小，够跑应用了)
# eclipse-temurin 是目前推荐的 OpenJDK 发行版
FROM eclipse-temurin:21-jre

# 安装字体管理工具 + 中文字体包
# fontconfig: 字体配置工具
# libfreetype6: 字体渲染引擎
# fonts-noto-cjk: Google Noto CJK 字体 (覆盖简繁中日韩)
# fonts-wqy-microhei: 文泉驿微米黑 (备用，体积小，兼容性好)
RUN apt-get update && \
    apt-get install -y fontconfig libfreetype6 fonts-noto-cjk fonts-wqy-microhei && \
    # 刷新字体缓存，确保 Java 能立刻识别到新安装的字体
    fc-cache -fv && \
    # 清理缓存减小镜像体积
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建后的 jar 文件到容器中
COPY target/KnowSeek-1.0-SNAPSHOT.jar app.jar

# 暴露 Spring Boot 默认端口
EXPOSE 8181

# 设置 JVM 参数 -XX:+UseContainerSupport 确保 JVM 感知 Docker 内存限制
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]