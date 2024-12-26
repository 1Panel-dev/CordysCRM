FROM registry.fit2cloud.com/metersphere/alpine-openjdk21-jre

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG CRM_VERSION=main
ARG MODULE=crm
ARG DEPENDENCY=backend/${MODULE}/target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

# web
COPY backend/${MODULE}/src/main/resources/packages /app/static

ENV JAVA_CLASSPATH=/app:/app/lib/*
ENV JAVA_MAIN_CLASS=io.cordys.Application
ENV AB_OFF=true
ENV CRM_VERSION=${CRM_VERSION}
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

RUN echo -n "${CRM_VERSION}" > /tmp/CRM_VERSION

CMD ["/deployments/run-java.sh"]
