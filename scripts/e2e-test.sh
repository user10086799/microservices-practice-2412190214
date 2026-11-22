#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."

read_env() {
  local key="$1"
  local current="$2"
  if [ -n "$current" ]; then
    printf '%s' "$current"
    return
  fi
  if [ -f .env ]; then
    sed -n "s/^${key}=//p" .env | head -n 1 | tr -d '\r'
  fi
}

USER_PORT="$(read_env USER_PORT "${USER_PORT:-}")"
PRODUCT_PORT="$(read_env PRODUCT_PORT "${PRODUCT_PORT:-}")"
PRODUCT_PORT_B="$(read_env PRODUCT_PORT_B "${PRODUCT_PORT_B:-}")"
ORDER_PORT="$(read_env ORDER_PORT "${ORDER_PORT:-}")"
USER_PORT="${USER_PORT:-8081}"
PRODUCT_PORT="${PRODUCT_PORT:-8082}"
PRODUCT_PORT_B="${PRODUCT_PORT_B:-18083}"
ORDER_PORT="${ORDER_PORT:-8083}"

USER_URL="http://127.0.0.1:${USER_PORT}"
PRODUCT_URL="http://127.0.0.1:${PRODUCT_PORT}"
PRODUCT_B_URL="http://127.0.0.1:${PRODUCT_PORT_B}"
ORDER_URL="http://127.0.0.1:${ORDER_PORT}"
BODY="${TMPDIR:-/tmp}/week8-e2e-body.json"
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
    curl -sS -o "$BODY" -w "%{http_code}" -X "$method" "$url" -H "Content-Type: application/json" --data "$data"
  else
    curl -sS -o "$BODY" -w "%{http_code}" -X "$method" "$url"
  fi
}

expect() {
  local actual="$1"
  local wanted="$2"
  local label="$3"
  if [ "$actual" = "$wanted" ]; then
    say "PASS $label -> $actual $(tr -d '\r\n' < "$BODY")"
  else
    say "FAIL $label -> $actual $(tr -d '\r\n' < "$BODY")"
    fail=1
  fi
}

json_field() {
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))[sys.argv[2]])' "$1" "$2"
}

order_count() {
  request GET "$ORDER_URL/api/orders" >/dev/null
  python3 -c 'import json,sys; print(len(json.load(open(sys.argv[1], encoding="utf-8"))))' "$BODY"
}

say "USER $USER_URL"
say "PRODUCT_A $PRODUCT_URL"
say "PRODUCT_B $PRODUCT_B_URL"
say "ORDER $ORDER_URL"

for pair in "user-service|$USER_URL" "product-a|$PRODUCT_URL" "product-b|$PRODUCT_B_URL" "order-service|$ORDER_URL"; do
  name="${pair%%|*}"
  url="${pair#*|}"
  code="$(request GET "$url/actuator/health")"
  expect "$code" "200" "$name health"
done

suffix="$(date +%H%M%S)"
user_code="$(request POST "$USER_URL/api/users" "{\"name\":\"week8-buyer-${suffix}\"}")"
expect "$user_code" "200" "create user"
user_id="$(json_field "$BODY" id)"

product_code="$(request POST "$PRODUCT_URL/api/products" "{\"name\":\"week8-goods-${suffix}\",\"description\":\"load balance\",\"price\":12.50,\"stock\":20}")"
expect "$product_code" "200" "create product"
product_id="$(json_field "$BODY" id)"

order_code="$(request POST "$ORDER_URL/api/orders" "{\"userId\":${user_id},\"productId\":${product_id},\"quantity\":1}")"
expect "$order_code" "200" "create order via OpenFeign"
say "ORDER_BODY $(tr -d '\r\n' < "$BODY")"

seen_a=0
seen_b=0
i=1
while [ "$i" -le 10 ]; do
  code="$(request GET "$ORDER_URL/api/orders/upstream/products/${product_id}")"
  instance="$(json_field "$BODY" instanceId)"
  say "PROBE $i -> $code instance=$instance"
  if [ "$code" != "200" ]; then
    fail=1
  fi
  if [ "$instance" = "product-a" ]; then
    seen_a=1
  fi
  if [ "$instance" = "product-b" ]; then
    seen_b=1
  fi
  i=$((i + 1))
done

if [ "$seen_a" = "1" ] && [ "$seen_b" = "1" ]; then
  say "PASS load-balance saw product-a and product-b"
else
  say "FAIL load-balance seen_a=$seen_a seen_b=$seen_b"
  fail=1
fi

before="$(order_count)"
say "STOP mysql-product"
docker compose stop mysql-product >/dev/null
user_code="$(request GET "$USER_URL/api/users/${user_id}")"
expect "$user_code" "200" "user still readable while product database is down"
list_code="$(request GET "$ORDER_URL/api/orders")"
expect "$list_code" "200" "orders still readable while product database is down"
bad_code="$(request POST "$ORDER_URL/api/orders" "{\"userId\":${user_id},\"productId\":${product_id},\"quantity\":1}")"
expect "$bad_code" "503" "order rejected when product database is down"
after="$(order_count)"
if [ "$before" = "$after" ]; then
  say "PASS order count stayed $after"
else
  say "FAIL order count changed $before -> $after"
  fail=1
fi

say "START mysql-product"
docker compose start mysql-product >/dev/null

if [ "$fail" = "0" ]; then
  say "E2E TEST PASSED"
  exit 0
fi
say "E2E TEST FAILED"
exit 1
