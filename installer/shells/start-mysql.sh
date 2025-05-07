#!/bin/sh

cp -rf /opt/cordys/conf/mysql/my.cnf /etc/my.cnf.d/mariadb-server.cnf

if [ ! -d "/run/mysqld" ]; then
  mkdir -p /run/mysqld
fi

if [ -d /app/mysql ]; then
  echo "[i] MySQL directory already present, skipping creation"
else
  echo "[i] MySQL data directory not found, creating initial DBs"

  mysql_install_db --user=root > /dev/null

  if [ "$CORDYS_MYSQL_PASSWORD" = "" ]; then
    CORDYS_MYSQL_PASSWORD=CordysCRM@mysql
    echo "[i] MySQL root Password: $CORDYS_MYSQL_PASSWORD"
  fi

  CORDYS_MYSQL_DB=${CORDYS_MYSQL_DB:-"cordys-crm"}
  CORDYS_MYSQL_USER=${CORDYS_MYSQL_USER:-""}
  CORDYS_MYSQL_PASSWORD=${CORDYS_MYSQL_PASSWORD:-""}

  tfile=`mktemp`
  if [ ! -f "$tfile" ]; then
      return 1
  fi

  cat << EOF > $tfile
USE mysql;
FLUSH PRIVILEGES;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY "$CORDYS_MYSQL_PASSWORD" WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
ALTER USER 'root'@'localhost' IDENTIFIED BY '';
EOF

  if [ "$CORDYS_MYSQL_DB" != "" ]; then
    echo "[i] Creating database: $CORDYS_MYSQL_DB"
    echo "CREATE DATABASE IF NOT EXISTS \`$CORDYS_MYSQL_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" >> $tfile

    if [ "$CORDYS_MYSQL_USER" != "" ]; then
      echo "[i] Creating user: $CORDYS_MYSQL_USER with password $CORDYS_MYSQL_PASSWORD"
      echo "GRANT ALL ON \`$CORDYS_MYSQL_DB\`.* to '$CORDYS_MYSQL_USER'@'%' IDENTIFIED BY '$CORDYS_MYSQL_PASSWORD';" >> $tfile
    fi
  fi

  /usr/bin/mysqld --user=root --bootstrap --verbose=0 < $tfile
  rm -f $tfile
fi


exec /usr/bin/mysqld --user=root --console