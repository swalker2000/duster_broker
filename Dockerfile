# ---------- build stage ----------
FROM eclipse-temurin:17-jdk AS build
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
FROM eclipse-temurin:17-jre
WORKDIR /app

# (опционально) если приложение слушает 8080
EXPOSE 8080

COPY --from=build /app/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]