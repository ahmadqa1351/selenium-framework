# ============================================================
# Dockerfile — Build a portable test execution image
#
# This image contains the framework code and Maven dependencies
# pre-cached for fast CI startup. The test target environment
# and browser are injected at runtime via --build-arg / -e.
#
# Build:
#   docker build -t selenium-framework:latest .
#
# Run (smoke, headless):
#   docker run --rm \
#     -e ENV=qa \
#     -e BROWSER=chrome_headless \
#     -e ADMIN_PASSWORD=secret \
#     -v $(pwd)/reports:/app/reports \
#     selenium-framework:latest
# ============================================================

# ── Stage 1: dependency cache ──────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS dependency-cache

WORKDIR /app
COPY pom.xml .
# Download all dependencies (cached as a Docker layer)
RUN mvn dependency:go-offline -q


# ── Stage 2: runtime image ─────────────────────────────────
FROM seleniarm/standalone-chromium:latest

# Switch to root for package installation
USER root

# Install Java 17 and Maven
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends \
        openjdk-17-jdk \
        maven \
        curl \
        unzip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copy pre-downloaded Maven repository from Stage 1
COPY --from=dependency-cache /root/.m2 /root/.m2

WORKDIR /app

# Copy framework source code
COPY pom.xml .
COPY src/ src/
COPY testng*.xml ./

# Environment variables (overridable at docker run time)
ENV ENV=qa
ENV BROWSER=chrome_headless
ENV SUITE=smoke
ENV THREAD_COUNT=2
ENV ADMIN_PASSWORD=""
ENV API_KEY=""

# Create report output directory (mount this as a volume to retrieve reports)
RUN mkdir -p /app/reports/screenshots /app/reports/logs

# Healthcheck: verify Java and Chrome are available
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -version && google-chrome --version || exit 1

ENTRYPOINT ["sh", "-c"]
CMD ["mvn test \
        -P ${SUITE} \
        -Denv=${ENV} \
        -Dbrowser=${BROWSER} \
        -Dparallel.thread.count=${THREAD_COUNT} \
        -Dadmin.password=${ADMIN_PASSWORD} \
        -Dapi.key=${API_KEY} \
        -q 2>&1 | tee /app/reports/logs/docker-run.log"]
