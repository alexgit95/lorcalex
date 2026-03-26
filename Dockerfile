# ─────────────────────────────────────────────────────────────
# Stage 1 : Build du backend Spring Boot
# Le frontend (HTML/JS/CSS vanilla) est dans src/main/resources/static
# et sera inclus automatiquement dans le JAR par Maven.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /app


# Copy Maven wrapper & POM first for dependency caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy sources and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ─────────────────────────────────────────────────────────────
# Stage 2 : Image finale légère (JRE seulement)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Non-root user for security
RUN addgroup -S lorcalex && adduser -S lorcalex -G lorcalex
USER lorcalex

COPY --from=backend-build /app/target/lorcalex-*.jar app.jar

EXPOSE 8181

# Profil docker → PostgreSQL
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
