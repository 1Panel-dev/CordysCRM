#!/bin/bash
set -e  # 遇到错误立即退出

# 日志函数
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# 创建必要目录
log "创建 Redis 数据目录..."
mkdir -p /opt/cordys/data/redis
mkdir -p /opt/cordys/conf/redis

# 检查 Redis 密码
if [ -z "${REDIS_PASSWORD}" ]; then
    log "警告：REDIS_PASSWORD 环境变量未设置，使用默认密码 CordysCRM@redis"
    REDIS_PASSWORD="CordysCRM@redis"
fi

# 启动 Redis 服务器
log "启动 Redis 服务器..."
redis-server /opt/cordys/conf/redis/redis.conf --requirepass "${REDIS_PASSWORD}"