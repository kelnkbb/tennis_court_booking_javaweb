# 使用 Maven 镜像作为构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 更新 CA 证书（解决 SSL 问题）
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates --fresh && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 复制 pom.xml 并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app

# 同样更新 JRE 的 CA 证书
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates --fresh && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]