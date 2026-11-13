#!/usr/bin/env bash
set -e
DAY=$(date +%F)
mysqldump --single-transaction -ushortand -p"$MYSQL_PASSWORD" shortand \
  | gzip > /opt/shortand/data/backup/shortand-$DAY.sql.gz
find /opt/shortand/data/backup -name 'shortand-*.sql.gz' -mtime +7 -delete
