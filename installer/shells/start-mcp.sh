#!/bin/sh
set -e

bash /shells/init-directories.sh

JAR_PATH="/app/mcp/cordys-crm-mcp-server.jar"

if [ ! -f "$JAR_PATH" ]; then
  echo "MCP JAR not found at $JAR_PATH"
  exit 1
fi

echo "Starting MCP Server..."
exec java -jar $JAVA_OPTIONS "$JAR_PATH"
