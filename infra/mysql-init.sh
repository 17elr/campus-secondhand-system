#!/bin/sh
# The image may source this script; keep shell options local.
(
set -eu
for value in "$MARKET_DB_PASSWORD" "$NOTICE_DB_PASSWORD"; do
 case "$value" in ''|*[!a-zA-Z0-9_-]*) echo 'Database passwords must use letters, numbers, underscore or hyphen' >&2; exit 1;; esac
done
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS market CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS notice CHARACTER SET utf8mb4;
CREATE USER 'market'@'%' IDENTIFIED BY '$MARKET_DB_PASSWORD';
CREATE USER 'notice'@'%' IDENTIFIED BY '$NOTICE_DB_PASSWORD';
GRANT ALL ON market.* TO 'market'@'%';
GRANT ALL ON notice.* TO 'notice'@'%';
SQL
)
