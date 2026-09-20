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
  "/opt/cordys/logs/mcp-server"
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
copy_conf "/installer/conf/cordys-crm.properties" "/opt/cordys/conf/cordys-crm.properties" "Cordys CRM"
copy_conf "/installer/conf/mysql/my.cnf"           "/opt/cordys/conf/mysql/my.cnf"        "MySQL"
copy_conf "/installer/conf/redis/redis.conf"       "/opt/cordys/conf/redis/redis.conf"    "Redis"

# 新安装生成随机密钥，升级时替换历史默认密钥，并保留用户已配置的密钥
secret_file="/opt/cordys/conf/cordys-crm.properties"
legacy_secret="9a9rdqPlTqhpZzkq"
current_secret=$(grep '^cordys.secret.key=' "$secret_file" | head -n 1 | cut -d'=' -f2- | tr -d '\r')
if [ -z "$current_secret" ] || [ "$current_secret" = "$legacy_secret" ]; then
  generated_secret=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
  if grep -q '^cordys.secret.key=' "$secret_file"; then
    sed -i "s|^cordys.secret.key=.*$|cordys.secret.key=${generated_secret}|" "$secret_file"
  else
    printf '\ncordys.secret.key=%s\n' "$generated_secret" >> "$secret_file"
  fi
  log "已生成随机 cordys.secret.key"
fi

# 仅在目录存在时再设置权限
if [ -d "/opt/cordys" ]; then
  log "设置目录权限: /opt/cordys"
  chmod -R 777 /opt/cordys
fi

log "目录初始化完成。"
