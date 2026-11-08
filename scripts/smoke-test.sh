#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."

USER_URL="${USER_URL:-http://localhost:${USER_PORT:-8081}}"
PRODUCT_URL="${PRODUCT_URL:-http://localhost:${PRODUCT_PORT:-8082}}"
ORDER_URL="${ORDER_URL:-http://localhost:${ORDER_PORT:-8083}}"
BODY="$(mktemp)"
fail=0

finish() {
  rm -f "$BODY"
}
trap finish EXIT

say() {
  printf '%s\n' "$*"
}

request() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  if [ -n "$data" ]; then
    curl -sS --connect-timeout 5 --max-time 20 -o "$BODY" -w "%{http_code}" -X "$method" "$url" -H "Content-Type: application/json" -d "$data" || true
  else
    curl -sS --connect-timeout 5 --max-time 20 -o "$BODY" -w "%{http_code}" -X "$method" "$url" || true
  fi
}

expect() {
  local actual="$1"
  local wanted="$2"
  local label="$3"
  if [ "$actual" = "$wanted" ]; then
    say "PASS $label -> $actual"
  else
    say "FAIL $label -> $actual, expected $wanted"
    fail=1
  fi
  say "$(cat "$BODY")"
}

json_id() {
  sed -n 's/.*"id":[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$BODY" | head -n 1
}

count_orders() {
  request GET "$ORDER_URL/api/orders" >/dev/null
  grep -o '"id"' "$BODY" | wc -l | tr -d '[:space:]'
}

say "===== health ====="
for item in "user-service|$USER_URL" "product-service|$PRODUCT_URL" "order-service|$ORDER_URL"; do
  name="${item%%|*}"
  url="${item#*|}"
  code="$(request GET "$url/actuator/health")"
  expect "$code" 200 "$name GET $url/actuator/health"
done

say "===== cross-service order ====="
code="$(request POST "$USER_URL/api/users" '{"name":"陈鑫涛"}')"
expect "$code" 200 "POST $USER_URL/api/users"
user_id="$(json_id)"
say "userId=$user_id"

code="$(request POST "$PRODUCT_URL/api/products" '{"name":"无线鼠标","description":"静音办公鼠标","price":89.00,"stock":20}')"
expect "$code" 200 "POST $PRODUCT_URL/api/products"
product_id="$(json_id)"
say "productId=$product_id"

code="$(request POST "$ORDER_URL/api/orders" "{\"userId\":$user_id,\"productId\":$product_id,\"quantity\":1}")"
expect "$code" 200 "POST $ORDER_URL/api/orders"
order_id="$(json_id)"
say "orderId=$order_id"

code="$(request GET "$ORDER_URL/api/orders/$order_id")"
expect "$code" 200 "GET $ORDER_URL/api/orders/$order_id"

code="$(request GET "$PRODUCT_URL/api/products/$product_id")"
expect "$code" 200 "GET $PRODUCT_URL/api/products/$product_id"

say "===== missing product ====="
mid_before="$(count_orders)"
code="$(request POST "$ORDER_URL/api/orders" "{\"userId\":$user_id,\"productId\":99999,\"quantity\":1}")"
expect "$code" 404 "POST $ORDER_URL/api/orders missing product"
mid_after="$(count_orders)"
if [ "$mid_before" = "$mid_after" ]; then
  say "PASS order count unchanged at $mid_after"
else
  say "FAIL order count changed from $mid_before to $mid_after"
  fail=1
fi

say "===== product service unavailable ====="
down_before="$(count_orders)"
docker compose stop product-service
code="$(request POST "$ORDER_URL/api/orders" "{\"userId\":$user_id,\"productId\":$product_id,\"quantity\":1}")"
expect "$code" 503 "POST $ORDER_URL/api/orders while product-service is stopped"
down_after="$(count_orders)"
if [ "$down_before" = "$down_after" ]; then
  say "PASS order count unchanged at $down_after"
else
  say "FAIL order count changed from $down_before to $down_after"
  fail=1
fi
docker compose start product-service
ready=0
for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do
  code="$(request GET "$PRODUCT_URL/actuator/health")"
  if [ "$code" = "200" ]; then
    ready=1
    break
  fi
  sleep 1
done
if [ "$ready" = "1" ]; then
  say "PASS product-service health recovered"
else
  say "FAIL product-service did not recover"
  fail=1
fi

if [ "$fail" = "0" ]; then
  say "SMOKE TEST PASSED"
  exit 0
fi
say "SMOKE TEST FAILED"
exit 1
