#!/bin/sh

bash /shells/init-directories.sh

MAIN_CLASS="cn.cordys.Application"
export CRM_VERSION=`cat /tmp/CRM_VERSION`

exec java ${JAVA_OPTIONS} -cp "/app/cordys-crm.jar:/app/lib/*" ${MAIN_CLASS}