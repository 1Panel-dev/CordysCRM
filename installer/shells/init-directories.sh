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

# 通用配置文件复制函数
copy_conf() {
  local source="$1"
  local target="$2"
  local name="$3"

  if [ ! -f "$target" ]; then
    log "$name 配置文件不存在，复制默认配置: $source -> $target"
    cp "$source" "$target"
  else
    log "$name 配置文件已存在: $target"
  fi
}

# 应用配置文件
copy_conf "../conf/cordys-crm.properties" "/opt/cordys/conf/cordys-crm.properties" "Cordys CRM"
copy_conf "../conf/mysql/my.cnf"           "/opt/cordys/conf/mysql/my.cnf"        "MySQL"
copy_conf "../conf/redis/redis.conf"       "/opt/cordys/conf/redis/redis.conf"    "Redis"

# 仅在目录存在时再设置权限
if [ -d "/opt/cordys" ]; then
  log "设置目录权限: /opt/cordys"
  chmod -R 777 /opt/cordys
fi

log "目录初始化完成。"