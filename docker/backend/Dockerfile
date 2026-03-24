# 使用 Maven 镜像作为构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 更新 CA 证书（解决 SSL 问题）
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates --fresh && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 复制 pom.xml 并下载依赖（go-offline 对网络更敏感；resolve + 重试更适合 Docker 构建）
COPY pom.xml .
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=10 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true"
RUN set -eux; \
  for i in 1 2 3 4 5; do \
    mvn -B -e -U dependency:resolve-plugins dependency:resolve && exit 0; \
    echo "maven dependency resolve attempt $i failed, retrying in ${i}0s..."; \
    sleep $((i * 10)); \
  done; \
  exit 1

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