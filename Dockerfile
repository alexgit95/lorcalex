# ─────────────────────────────────────────────────────────────
# Stage 1 : Build du backend Spring Boot
# Le frontend (HTML/JS/CSS vanilla) est dans src/main/resources/static
# et sera inclus automatiquement dans le JAR par Maven.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.4_7-jdk AS backend-build
WORKDIR /app

ARG GIT_COMMIT=unknown

COPY .mvn .mvn
# Copy Maven wrapper & POM first for dependency caching
COPY mvnw mvnw.cmd pom.xml ./


# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy sources and build
COPY src ./src
RUN ./mvnw package -DskipTests -B -Dgit.commit=${GIT_COMMIT}

# ─────────────────────────────────────────────────────────────
# Stage 2 : Image finale légère (JRE seulement)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25.0.4_7-jre
WORKDIR /app



COPY --from=backend-build /app/target/lorcalex-*.jar app.jar

RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser
USER appuser

EXPOSE 8181

ENV APP_TIMEZONE=Europe/Paris

# Profil docker → PostgreSQL
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Europe/Paris", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
