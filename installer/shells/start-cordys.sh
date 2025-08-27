#!/bin/sh

mkdir -p /opt/cordys/logs/cordys-crm
mkdir -p /opt/cordys/data/files

chmod -R 777 /opt/cordys

export JAVA_CLASSPATH=/app:/app/lib/*
export JAVA_MAIN_CLASS=cn.cordys.Application
export CRM_VERSION=`cat /tmp/CRM_VERSION`

sh /deployments/run-java.sh