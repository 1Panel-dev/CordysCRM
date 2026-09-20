#!/bin/sh
set -e

APP_DIR=/app/cockpit
ENTRYPOINT="$APP_DIR/dist/http/server.js"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js runtime not found"
  exit 1
fi

if [ ! -f "$ENTRYPOINT" ]; then
  echo "Cockpit application not found at $ENTRYPOINT"
  exit 1
fi

export NODE_ENV="${NODE_ENV:-production}"
export PORT="${PORT:-8088}"
export CORDYS_CRM_URL="${CORDYS_CRM_URL:-http://127.0.0.1:8081}"
export CORDYS_MCP_URL="${CORDYS_MCP_URL:-http://127.0.0.1:8082/mcp}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-CordysCRM@redis}"

cd "$APP_DIR"
echo "Starting Cockpit Server..."
exec node "$ENTRYPOINT"
