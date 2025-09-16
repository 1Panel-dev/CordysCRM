#!/bin/bash

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

start_mcp() {
    log_info "启动 MCP 服务..."
    /shells/start-mcp.sh &
    if /shells/wait-for-it.sh 127.0.0.1:8082 --timeout=120 --strict; then
        log_info "MCP 服务已就绪"
        return 0
    else
        log_error "MCP 服务启动失败或超时"
        return 1
    fi
}

get_property() {
    local key=$1
    grep "^${key}=" /opt/cordys/conf/cordys-crm.properties | cut -d'=' -f2 | tr -d '[:space:]'
}

# 主函数
main() {
    log_info "开始启动 Cordys CRM 环境..."
    bash /shells/init-directories.sh

    # 检查MySQL配置并启动
    mysqlEmbeddedEnabled=$(get_property "mysql.embedded.enabled")
    if [[ "${mysqlEmbeddedEnabled}" == "true" ]]; then
        log_info "启动内置 MySQL ... "
        start_mysql
    else
        log_info "使用外部 MySQL 服务"
    fi

    # 检查Redis配置并启动
    redisEmbeddedEnabled=$(get_property "redis.embedded.enabled")
    if [[ "${redisEmbeddedEnabled}" == "true" ]]; then
        log_info "启动内置 Redis ... "
        start_redis
    else
        log_info "使用外部 Redis 服务"
    fi

    # 启动 Cordys CRM 应用
    log_info "启动 Crodys CRM 应用 ..."
    sh /shells/start-cordys.sh

    # 启动 MCP 服务
    # 检查Redis配置并启动
   mcpEmbeddedEnabled=$(get_property "mcp.embedded.enabled")
   if [[ "${mcpEmbeddedEnabled}" == "true" ]]; then
       log_info "启动内置 MCP ... "
       start_mcp
   else
       log_info "使用外部 MCP 服务"
   fi
}

# 执行主函数
main