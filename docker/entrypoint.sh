#!/bin/sh
mkdir -p /app/config
echo "$SPRING_CONFIG" > /app/config/application-overrides.yml

exec "$@"
