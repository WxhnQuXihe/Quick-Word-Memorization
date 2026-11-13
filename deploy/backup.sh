#!/usr/bin/env bash
set -e
DAY=$(date +%F)
mysqldump --single-transaction -ushorthand -p"$MYSQL_PASSWORD" shorthand \
  | gzip > /opt/shorthand/data/backup/shorthand-$DAY.sql.gz
find /opt/shorthand/data/backup -name 'shorthand-*.sql.gz' -mtime +7 -delete
