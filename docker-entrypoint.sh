#!/bin/sh
set -eu

# Render exposes PostgreSQL as postgresql://user:password@host:port/database.
# Spring JDBC requires jdbc:postgresql://host:port/database and receives the
# generated username/password through separate environment variables.
if [ -n "${DATABASE_URL:-}" ]; then
    case "$DATABASE_URL" in
        postgresql://*)
            database_address=${DATABASE_URL#postgresql://}
            database_address=${database_address#*@}
            export DB_URL="jdbc:postgresql://${database_address}"
            ;;
        jdbc:postgresql://*)
            export DB_URL="$DATABASE_URL"
            ;;
        *)
            echo "Unsupported DATABASE_URL scheme" >&2
            exit 1
            ;;
    esac
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar \
    --server.address=0.0.0.0 \
    --server.port="${PORT:-8080}"
