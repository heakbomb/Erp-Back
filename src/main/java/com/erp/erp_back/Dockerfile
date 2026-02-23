# 1) Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 의존성 캐시 최적화 (pom.xml 먼저 복사)
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# 소스 복사 후 빌드
COPY . .
RUN mvn -B -DskipTests clean package

# 2) Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# 보안: non-root 실행
RUN useradd -m appuser
USER appuser

# jar 복사 (target/*.jar 중 1개를 app.jar로)
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]