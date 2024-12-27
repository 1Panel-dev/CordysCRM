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
RUN ./mvnw clean package -DskipTests -pl '!frontend' && \
    mkdir backend/crm/target/dependency && \
    cd backend/crm/target/dependency && \
    jar -xf ../*.jar



FROM registry.fit2cloud.com/cordys/cordys-base

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG CRM_VERSION=main
ARG MODULE=crm
ARG DEPENDENCY=/build/backend/${MODULE}/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build /build/backend/${MODULE}/src/main/resources/packages /app/static
COPY installer/conf /opt/cordys/conf
COPY installer/shells /shells

ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=io.cordys.Application
ENV AB_OFF=true
ENV CRM_VERSION=${CRM_VERSION}
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

VOLUME [ "/opt/cordys/data", "/opt/cordys/logs", "/opt/cordys/conf" ]

ENTRYPOINT ["sh", "/shells/start-all.sh"]
