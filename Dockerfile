FROM eclipse-temurin:21-jre

WORKDIR /app
RUN useradd --system --uid 10001 app

# 워크플로가 bootJar 결과물을 build/libs/app.jar로 복사해 둔다
COPY build/libs/app.jar app.jar

USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
