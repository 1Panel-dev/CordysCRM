#!/bin/sh

dburl=$(cat /opt/cordys/conf/cordys.properties | grep 'spring.datasource.url')
if [[ "${dburl}" == *"127.0.0.1"* ]]; then
    cp -rf /opt/cordys/conf/mysql/my.cnf /etc/my.cnf.d/mariadb-server.cnf
    /shells/run-mysql.sh &
    /shells/wait-for-it.sh 127.0.0.1:3306 --timeout=120 --strict
fi


redishost=$(cat /opt/cordys/conf/redisson.yml | grep 'address:')
if [[ "${redishost}" == *"127.0.0.1"* ]]; then
    mkdir -p /opt/cordys/data/redis
    redis-server /opt/cordys/conf/redis/redis.conf &
    /shells/wait-for-it.sh 127.0.0.1:6379 --timeout=120 --strict
fi


chmod -R 777 /opt/cordys/logs


export JAVA_CLASSPATH=/app:/app/lib/*
export JAVA_MAIN_CLASS=io.cordys.Application
export CRM_VERSION=`cat /tmp/CRM_VERSION`

sh /deployments/run-java.sh


