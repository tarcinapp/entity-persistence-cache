####
# Multi-stage build for entity-persistence-cache
#
# Stage 1: build the Quarkus uber-jar
# Stage 2: run with a minimal JRE 21 image
####

# ── Build stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /build
COPY . .
RUN ./mvnw -B package -DskipTests

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Non-root user for security
RUN addgroup --system epc && adduser --system --ingroup epc epc
USER epc

COPY --from=build /build/target/quarkus-app/lib /app/lib
COPY --from=build /build/target/quarkus-app/app /app/app
COPY --from=build /build/target/quarkus-app/quarkus /app/quarkus
COPY --from=build /build/target/quarkus-app/quarkus-run.jar /app/quarkus-run.jar

EXPOSE 3001

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/quarkus-run.jar"]
