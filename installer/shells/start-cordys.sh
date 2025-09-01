#!/bin/sh

sh /shells/init-directories.sh

export JAVA_CLASSPATH=/app:/app/lib/*
export JAVA_MAIN_CLASS=cn.cordys.Application
export CRM_VERSION=`cat /tmp/CRM_VERSION`

sh /deployments/run-java.sh