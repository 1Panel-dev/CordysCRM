#!/bin/sh

chmod -R 777 /opt/cordys

export JAVA_CLASSPATH=/app:/app/lib/*
export JAVA_MAIN_CLASS=io.cordys.Application
export CRM_VERSION=`cat /tmp/CRM_VERSION`

sh /deployments/run-java.sh


