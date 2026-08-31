ARG MAVEN_IMAGE=maven:3.8.8-eclipse-temurin-8
ARG JAVA_IMAGE=eclipse-temurin:8-jdk-jammy

FROM ${MAVEN_IMAGE} AS build

WORKDIR /workspace
COPY pom.xml ./
COPY docker/backend/settings.xml /root/.m2/settings.xml
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
# Tests run in CI before image publication. Skip them in the image build so
# packaging does not require a developer-local MySQL instance.
RUN mvn -B -ntp -DskipTests clean package

FROM ${JAVA_IMAGE}

RUN sed -i \
        -e 's|http://archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        -e 's|http://security.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        /etc/apt/sources.list \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y gcc python3 \
    && ln -s /usr/bin/python3 /usr/local/bin/python \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads \
    && chown -R app:app /app

USER app
EXPOSE 8080

# The runtime image contains bash; this checks that Tomcat is accepting TCP connections.
HEALTHCHECK --interval=10s --timeout=3s --start-period=35s --retries=10 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
