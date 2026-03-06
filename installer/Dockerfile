FROM --platform=${BUILDPLATFORM} eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . /build

RUN ./mvnw clean package -DskipTests

FROM ghcr.io/cordys-dev/cordys-base:latest

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG MODULE=app

COPY --from=build /build/backend/${MODULE}/target/app-main.jar /app/cordys-crm.jar
COPY installer/shells /shells
COPY installer/conf /installer/conf
COPY installer/mcp /app/mcp

RUN mkdir -p /app/lib
ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

ARG CRM_VERSION=main
ENV CRM_VERSION=${CRM_VERSION}

RUN echo "CRM_VERSION value during build: ${CRM_VERSION}"
RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

VOLUME [ "/opt/cordys" ]

EXPOSE 8081 8082 3306

RUN chmod 777 /shells/*.sh

ENTRYPOINT ["sh", "/shells/start-all.sh"]