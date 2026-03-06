#!/bin/sh

bash /shells/init-directories.sh

export JAVA_CLASSPATH=/app:/app/lib/*
export CRM_VERSION=`cat /tmp/CRM_VERSION`

exec java ${JAVA_OPTIONS} -cp /app:/app/lib/* -jar /app/cordys-crm.jar