#!/bin/bash
set -e

# 日志函数
log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# 需要确保存在的目录
DIRS=(
  "/opt/cordys/data/mysql"
  "/opt/cordys/conf/mysql"
  "/opt/cordys/logs/cordys-crm"
  "/opt/cordys/data/files"
  "/opt/cordys/data/redis"
  "/opt/cordys/conf/redis"
)

log "开始检查并创建必要目录..."
for d in "${DIRS[@]}"; do
  if [ ! -d "$d" ]; then
    log "创建目录: $d"
    mkdir -p "$d"
  else
    log "目录已存在: $d"
  fi
done

# 仅在目录存在时再设置权限
if [ -d "/opt/cordys" ]; then
  log "设置目录权限: /opt/cordys"
  chmod -R 777 /opt/cordys
fi

log "目录初始化完成。"