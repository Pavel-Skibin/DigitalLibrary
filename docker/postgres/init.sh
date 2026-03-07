#!/bin/bash
set -e

# Creates application user and three isolated databases.
# Env vars DB_USERNAME and DB_PASSWORD must be passed to the postgres container.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER ${DB_USERNAME} WITH PASSWORD '${DB_PASSWORD}';

    CREATE DATABASE users_db             OWNER ${DB_USERNAME};
    CREATE DATABASE books_db             OWNER ${DB_USERNAME};
    CREATE DATABASE comments_ratings_db  OWNER ${DB_USERNAME};
EOSQL
