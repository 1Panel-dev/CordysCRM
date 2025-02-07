FROM node:22-slim AS frontend

WORKDIR /frontend
COPY frontend /frontend
RUN npm install -g pnpm && \
    pnpm install && \
    pnpm run build


FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . /build
COPY --from=frontend /frontend/packages/web/dist /build/frontend/packages/web/dist
COPY --from=frontend /frontend/packages/mobile/dist /build/frontend/packages/mobile/dist

RUN JAR_VERSION=$(./mvnw help:evaluate -Dexpression=revision -q -DforceStdout) && \
    if [ -z "$JAR_VERSION" ]; then \
        JAR_VERSION="1.0.x"; \
    fi && \
    echo "Using Jar version: $JAR_VERSION" && \
   ./mvnw clean package -DskipTests -pl '!frontend' && \
    mkdir backend/app/target/dependency && \
    cd backend/app/target/dependency && \
    jar -xf ../*.jar


ARG FIT2CLOUD_MAVEN_USERNAME
ARG FIT2CLOUD_MAVEN_PASSWORD
ARG CRM_VERSION=main
ENV FIT2CLOUD_MAVEN_USERNAME=${FIT2CLOUD_MAVEN_USERNAME}
ENV FIT2CLOUD_MAVEN_PASSWORD=${FIT2CLOUD_MAVEN_PASSWORD}
ENV CRM_VERSION=${CRM_VERSION}

RUN ./mvnw deploy:deploy-file \
    -Dfile=backend/crm/target/crm-1.0.x.jar \
    -DrepositoryId=cordys-public \
    -Durl=https://repository.fit2cloud.com/repository/cordys/ \
    -DgroupId=io.cordys \
    -DartifactId=cordys-crm \
    -Dversion=1.0.x \
    -Dpackaging=jar \
    -Dusername=${FIT2CLOUD_MAVEN_USERNAME} \
    -Dpassword=${FIT2CLOUD_MAVEN_PASSWORD}


FROM ghcr.io/cordys-dev/cordys-base

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG MODULE=app
ARG DEPENDENCY=/build/backend/${MODULE}/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build /build/backend/${MODULE}/src/main/resources/static /app/static
COPY installer/conf /opt/cordys/conf
COPY installer/shells /shells

ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=io.cordys.Application
ENV AB_OFF=true
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

VOLUME [ "/opt/cordys/data", "/opt/cordys/logs", "/opt/cordys/conf" ]

ENTRYPOINT ["sh", "/shells/start-all.sh"]
