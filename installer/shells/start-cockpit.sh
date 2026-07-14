#!/bin/sh
set -e

bash /shells/init-directories.sh

JAR_PATH="/app/cockpit/cordys-crm-cockpit.jar"

if [ ! -f "$JAR_PATH" ]; then
  echo "Cockpit JAR not found at $JAR_PATH"
  exit 1
fi

echo "Starting Cockpit Server..."
exec java -jar $JAVA_OPTIONS "$JAR_PATH"
