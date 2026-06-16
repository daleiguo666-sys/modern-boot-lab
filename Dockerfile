# 多阶段构建：构建阶段用完整 JDK，运行阶段只带 JRE，最终镜像更小、更安全。

# ---- 阶段1：构建 ----（有完整 JDK + 源码 + Gradle，体积大，但不会进最终镜像）
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# 只打可执行 jar，不跑测试（测试已在 CI 跑过）。--no-daemon：容器里一次性构建，别留后台进程。
RUN ./gradlew clean bootJar --no-daemon

# ---- 阶段2：运行 ----（全新精简基础镜像，只有 JRE）
FROM eclipse-temurin:21-jre
WORKDIR /app
# 关键：只从构建阶段「拷出那个 jar」，JDK/源码/Gradle 缓存全留在阶段1，不进最终镜像。
# 上面跑了 clean bootJar，build/libs 下只会有一个可执行 jar，*.jar 不会误匹配。
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
