#!/bin/bash
set -e

# ------------------------------
# 日志函数
# ------------------------------
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# ------------------------------
# 启动内置服务函数
# ------------------------------
start_mysql() {
    log_info "启动 MySQL 服务..."
    /shells/start-mysql.sh &

    if /shells/wait-for-it.sh 127.0.0.1:3306 --timeout=120 --strict; then
        log_info "MySQL 服务已就绪"
        return 0
    else
        log_error "MySQL 服务启动失败或超时"
        exit 1
    fi
}

start_redis() {
    log_info "启动 Redis 服务..."
    /shells/start-redis.sh &

    if /shells/wait-for-it.sh 127.0.0.1:6379 --timeout=120 --strict; then
        log_info "Redis 服务已就绪"
        return 0
    else
        log_error "Redis 服务启动失败或超时"
        exit 1
    fi
}

start_mcp_after_cordys() {
      mcpEmbeddedEnabled=$(get_property "mcp.embedded.enabled")
      if [[ "${mcpEmbeddedEnabled}" == "true" ]]; then
          log_info "启动内置 MCP 服务 ..."
          /shells/wait-for-it.sh 127.0.0.1:8081 --timeout=120 --strict

          log_info "Cordys CRM 已就绪，启动 MCP..."
          sh /shells/start-mcp.sh &
          /shells/wait-for-it.sh 127.0.0.1:8082 --timeout=120 --strict
          log_info "MCP 已就绪"
      else
          log_info "使用外部 MCP 服务"
      fi
}

# ------------------------------
# 获取配置函数
# ------------------------------
get_property() {
    local key=$1
    grep "^${key}=" /opt/cordys/conf/cordys-crm.properties | cut -d'=' -f2 | tr -d '[:space:]'
}

# ------------------------------
# 主函数
# ------------------------------
main() {
    log_info "开始启动 Cordys CRM 环境..."

    # 初始化目录
    bash /shells/init-directories.sh

    # ------------------------------
    # MySQL
    # ------------------------------
    mysqlEmbeddedEnabled=$(get_property "mysql.embedded.enabled")
    if [[ "${mysqlEmbeddedEnabled}" == "true" ]]; then
        log_info "启动内置 MySQL ..."
        start_mysql
    else
        log_info "使用外部 MySQL 服务"
    fi

    # ------------------------------
    # Redis
    # ------------------------------
    redisEmbeddedEnabled=$(get_property "redis.embedded.enabled")
    if [[ "${redisEmbeddedEnabled}" == "true" ]]; then
        log_info "启动内置 Redis ..."
        start_redis
    else
        log_info "使用外部 Redis 服务"
    fi

    start_mcp_after_cordys &
    sh /shells/start-cordys.sh

    log_info "Cordys CRM 环境启动完成！"
}

# ------------------------------
# 执行主函数
# ------------------------------
main
