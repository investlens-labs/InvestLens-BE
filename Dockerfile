FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system investlens \
    && useradd --system --gid investlens --no-create-home investlens

COPY --from=builder --chown=investlens:investlens /workspace/build/libs/investlens-0.1.0.jar app.jar
COPY --chown=investlens:investlens docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod 0555 /app/docker-entrypoint.sh

USER investlens

EXPOSE 10000

ENTRYPOINT ["/app/docker-entrypoint.sh"]
