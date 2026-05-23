#!/bin/sh
set -eu

API_URL="${API_URL:-http://localhost:8080}"
CONFIG_PATH="/usr/share/nginx/html/assets/runtime-config.json"

mkdir -p "$(dirname "$CONFIG_PATH")"
printf '{"apiBaseUrl":"%s"}\n' "$API_URL" > "$CONFIG_PATH"

exec nginx -g 'daemon off;'
