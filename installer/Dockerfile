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
    mkdir backend/app/target/dependency && \
    cd backend/app/target/dependency && \
    jar -xf ../*.jar


ARG FIT2CLOUD_MAVEN_USERNAME
ARG FIT2CLOUD_MAVEN_PASSWORD

ENV FIT2CLOUD_MAVEN_USERNAME=${FIT2CLOUD_MAVEN_USERNAME}
ENV FIT2CLOUD_MAVEN_PASSWORD=${FIT2CLOUD_MAVEN_PASSWORD}

RUN JAR_VERSION=$(./mvnw help:evaluate -Dexpression=revision -q -DforceStdout || echo "1.0.x") && \
    echo "Using Jar version: $JAR_VERSION" && \
    ./mvnw deploy:deploy-file \
    -Dfile=backend/crm/target/crm-${JAR_VERSION}.jar \
    -DrepositoryId=cordys \
    -Durl=https://repository.fit2cloud.com/repository/cordys/ \
    -DgroupId=io.cordys \
    -DartifactId=crm \
    -Dversion=${JAR_VERSION} \
    -Dpackaging=jar \
    -s=./.mvn/wrapper/settings.xml

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

ARG CRM_VERSION=main
ENV CRM_VERSION=${CRM_VERSION}

RUN echo "CRM_VERSION value during build: ${CRM_VERSION}"
RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

VOLUME [ "/opt/cordys/data", "/opt/cordys/logs", "/opt/cordys/conf" ]

#ENTRYPOINT ["sh", "/shells/start-all.sh"]

# 添加环境变量，默认为true表示执行启动脚本
ARG EXECUTE_START_SCRIPT=true
ENV EXECUTE_START_SCRIPT=${EXECUTE_START_SCRIPT}

# 修改ENTRYPOINT使用条件判断（支持多种true值形式）
ENTRYPOINT ["/bin/sh", "-c", "if [ \"${EXECUTE_START_SCRIPT}\" = \"true\" ] || [ \"${EXECUTE_START_SCRIPT}\" = \"TRUE\" ] || [ \"${EXECUTE_START_SCRIPT}\" = \"1\" ]; then sh /shells/start-all.sh; else echo 'Startup script execution skipped' && tail -f /dev/null; fi"]