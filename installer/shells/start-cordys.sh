#!/bin/sh

bash /shells/init-directories.sh

export CRM_VERSION=`cat /tmp/CRM_VERSION`

exec java ${JAVA_OPTIONS} -cp "/app/cordys-crm.jar:/app/lib/*" org.springframework.boot.loader.JarLauncher