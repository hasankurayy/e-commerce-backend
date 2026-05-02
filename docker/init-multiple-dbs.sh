#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE ecommerce_user_db;
    CREATE DATABASE ecommerce_product_db;
    CREATE DATABASE ecommerce_cart_db;
    CREATE DATABASE ecommerce_order_db;
    CREATE DATABASE ecommerce_payment_db;
    CREATE DATABASE ecommerce_notification_db;
EOSQL

echo "Databases created successfully."
