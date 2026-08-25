# syntax=docker/dockerfile:1
# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Сначала копируем файлы сборки для лучшего кеширования слоёв
COPY gradlew build.gradle settings.gradle /app/
COPY gradle /app/gradle
RUN chmod +x /app/gradlew

# Копируем исходники
COPY src /app/src

# Собираем Spring Boot jar
RUN ./gradlew clean bootJar --no-daemon


# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

EXPOSE 8080
EXPOSE 9091
EXPOSE 9092

COPY --from=build /app/build/libs/*.jar /app/app.jar

# ./cert из контекста сборки → /app/certs. Нет папки — слой пустой, сборка не падает.
RUN mkdir -p /app/certs
RUN --mount=type=bind,target=/src \
    if [ -d /src/cert ]; then cp -a /src/cert/. /app/certs/; fi

ENTRYPOINT ["java","-jar","/app/app.jar"]