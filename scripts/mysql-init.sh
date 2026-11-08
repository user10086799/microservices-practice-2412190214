#!/bin/sh
set -eu
mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS shopping_user CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS shopping_product CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS shopping_order CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'user_app'@'%' IDENTIFIED BY '${USER_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'product_app'@'%' IDENTIFIED BY '${PRODUCT_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'order_app'@'%' IDENTIFIED BY '${ORDER_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON shopping_user.* TO 'user_app'@'%';
GRANT ALL PRIVILEGES ON shopping_product.* TO 'product_app'@'%';
GRANT ALL PRIVILEGES ON shopping_order.* TO 'order_app'@'%';
FLUSH PRIVILEGES;
EOF
