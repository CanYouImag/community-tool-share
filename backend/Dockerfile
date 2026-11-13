# 邻租后端：Maven 多阶段构建 → JRE 17 运行
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# 中国网络环境：Maven 走阿里云镜像加速依赖下载
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
# 先拉依赖，利用 Docker 层缓存
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre
WORKDIR /app
# jar 名与 pom 的 artifactId/version 一致；如改版本请同步此处
COPY --from=build /app/target/linzu-backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
