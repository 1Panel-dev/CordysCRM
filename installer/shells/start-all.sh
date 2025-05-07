#!/bin/bash

# 启动 CORDYS CRM应用及其依赖服务(MySQL 和 Redis)

# 日志函数
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# 启动MySQL服务
start_mysql() {
    log_info "启动 MySQL 服务..."
    /shells/start-mysql.sh &
    if /shells/wait-for-it.sh 127.0.0.1:3306 --timeout=120 --strict; then
        log_info "MySQL 服务已就绪"
        return 0
    else
        log_error "MySQL 服务启动失败或超时"
        return 1
    fi
}

# 启动Redis服务
start_redis() {
    log_info "启动 Redis 服务..."
    /shells/start-redis.sh &
    if /shells/wait-for-it.sh 127.0.0.1:6379 --timeout=120 --strict; then
        log_info "Redis 服务已就绪"
        return 0
    else
        log_error "Redis 服务启动失败或超时"
        return 1
    fi
}

# 主函数
main() {
    log_info "开始启动CORDYS CRM环境..."

    # 检查MySQL配置并启动
    dbUrl=$(grep 'spring.datasource.url' /opt/cordys/conf/cordys-crm.properties)
    if [[ "${dbUrl}" == *"127.0.0.1"* ]]; then
        log_info "检测到本地MySQL配置"
        start_mysql
    else
        log_info "使用外部MySQL服务"
    fi

    # 检查Redis配置并启动
    redisHost=$(grep 'spring.data.redis.host' /opt/cordys/conf/cordys-crm.properties)
    if [[ "${redisHost}" == *"127.0.0.1"* ]]; then
        log_info "检测到本地 Redis 配置"
        start_redis
    else
        log_info "使用外部 Redis 服务"
    fi

    # 启动CORDYS应用
    log_info "启动 CORDYS 应用..."
    sh /shells/start-cordys.sh
}

# 执行主函数
main