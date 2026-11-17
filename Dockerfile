# --- Stage 1: build the jar (needs the full JDK + Maven) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first: this layer is cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

# Then the source. Tests run outside Docker (they need Docker themselves).
COPY src src
RUN ./mvnw -q package -DskipTests

# --- Stage 2: run it (only the small Java runtime + the jar) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root
RUN useradd --system --uid 1001 app
USER app

COPY --from=build /workspace/target/mini-wsa-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
