FROM --platform=${BUILDPLATFORM} node:22-slim AS frontend

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
    mkdir backend/app/target/dependency && \
    cd backend/app/target/dependency && \
    jar -xf ../*.jar

FROM ghcr.io/cordys-dev/cordys-base

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG MODULE=app
ARG DEPENDENCY=/build/backend/${MODULE}/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY --from=build /build/backend/${MODULE}/src/main/resources/static /app/static
COPY installer/shells /shells
COPY installer/conf /installer/conf

# MCP JAR 拷贝到 lib
RUN if [ -f installer/mcp-server.jar ]; then \
        cp installer/mcp-server.jar /app/mcp/cordys-crm-mcp-server.jar; \
    fi

ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=cn.cordys.Application
ENV AB_OFF=true
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

ARG CRM_VERSION=main
ENV CRM_VERSION=${CRM_VERSION}

RUN echo "CRM_VERSION value during build: ${CRM_VERSION}"
RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

VOLUME [ "/opt/cordys" ]

EXPOSE 8081 8082 3306

RUN chmod 777  /shells/*.sh
ENTRYPOINT ["sh", "/shells/start-all.sh"]