FROM registry.fit2cloud.com/metersphere/alpine-openjdk21-jre

LABEL maintainer="FIT2CLOUD <support@fit2cloud.com>"

ARG MS_VERSION=dev
ARG MODULE=cordys-crm
ARG DEPENDENCY=backend/${MODULE}/target/dependency

COPY ${DEPENDENCY}/BOOT-INF/lib /${MODULE}/lib
COPY ${DEPENDENCY}/META-INF /${MODULE}/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /${MODULE}

# 静态文件
COPY backend/${MODULE}/src/main/resources/static /${MODULE}/static
ADD frontend/public /${MODULE}/static


ENV JAVA_CLASSPATH=/${MODULE}:/${MODULE}/lib/*
ENV JAVA_MAIN_CLASS=io.cordys.Application
ENV AB_OFF=true
ENV MS_VERSION=${MS_VERSION}
ENV JAVA_OPTIONS="-Dfile.encoding=utf-8 -Djava.awt.headless=true --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

RUN echo -n "${MS_VERSION}" > /tmp/MS_VERSION

CMD ["/deployments/run-java.sh"]
