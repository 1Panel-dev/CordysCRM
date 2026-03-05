#!/bin/sh

bash /shells/init-directories.sh

export CRM_VERSION=`cat /tmp/CRM_VERSION`

exec java ${JAVA_OPTIONS} -jar /app/cordys-crm.jar