#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://investlens-be.onrender.com}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-https://investlens.mandoo4137-a53.workers.dev}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

curl --fail --silent --show-error --retry 30 --retry-delay 10 --retry-all-errors \
  "$BASE_URL/actuator/health" > "$TMP_DIR/health.json"
jq -e '.status == "UP"' "$TMP_DIR/health.json" >/dev/null

curl --fail --silent --show-error --retry 30 --retry-delay 10 --retry-all-errors \
  "$BASE_URL/v3/api-docs" > "$TMP_DIR/openapi.json"
jq -e '
  .paths["/api/v1/instruments"] != null and
  .paths["/api/v1/instruments/{instrumentId}/news"] != null and
  .paths["/api/v1/instruments/{instrumentId}/news/sentiment"] != null
' "$TMP_DIR/openapi.json" >/dev/null

curl --silent --show-error --output /dev/null --dump-header "$TMP_DIR/cors.headers" \
  --request OPTIONS "$BASE_URL/api/v1/instruments?limit=1" \
  --header "Origin: $FRONTEND_ORIGIN" \
  --header 'Access-Control-Request-Method: GET'
grep -Fqi "access-control-allow-origin: $FRONTEND_ORIGIN" "$TMP_DIR/cors.headers"

protected_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
  "$BASE_URL/api/v1/portfolio")"
test "$protected_status" = "401"

echo "Production smoke test passed for $BASE_URL"
