# ─────────────────────────────────────────────────────────────
# Stage 1 : Build du backend Spring Boot
# Le frontend (HTML/JS/CSS vanilla) est dans src/main/resources/static
# et sera inclus automatiquement dans le JAR par Maven.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /app

RUN apk add --no-cache maven

# Cache des dépendances Maven
COPY pom.xml ./pom.xml
RUN mvn dependency:go-offline -q 2>/dev/null || true

# Sources Java + ressources statiques
COPY src ./src

RUN mvn package -DskipTests

# ─────────────────────────────────────────────────────────────
# Stage 2 : Image finale légère (JRE seulement)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=backend-build /app/target/lorcalex-*.jar app.jar

EXPOSE 8080

# Profil docker → PostgreSQL
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
