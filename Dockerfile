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

COPY --from=build /app/build/libs/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]