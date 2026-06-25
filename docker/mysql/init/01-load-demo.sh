#!/bin/sh
set -eu

database="${MYSQL_DATABASE:-car_dealer_crm}"
source_sql="/docker-entrypoint-demo/CarDealerCRM.sql"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${database}\`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
SQL

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" "${database}" < "${source_sql}"
