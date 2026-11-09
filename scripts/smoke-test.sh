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
ORDER_PORT="$(read_env ORDER_PORT "${ORDER_PORT:-}")"
NACOS_CONSOLE_PORT="$(read_env NACOS_CONSOLE_PORT "${NACOS_CONSOLE_PORT:-}")"
USER_PORT="${USER_PORT:-8081}"
PRODUCT_PORT="${PRODUCT_PORT:-8082}"
ORDER_PORT="${ORDER_PORT:-8083}"
NACOS_CONSOLE_PORT="${NACOS_CONSOLE_PORT:-18080}"

USER_URL="${USER_URL:-http://127.0.0.1:${USER_PORT:-8081}}"
PRODUCT_URL="${PRODUCT_URL:-http://127.0.0.1:${PRODUCT_PORT:-8082}}"
ORDER_URL="${ORDER_URL:-http://127.0.0.1:${ORDER_PORT:-8083}}"
NACOS_CONSOLE_URL="${NACOS_CONSOLE_URL:-http://127.0.0.1:${NACOS_CONSOLE_PORT:-18080}/}"
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
  curl -sS --connect-timeout 5 --max-time 20 -o "$BODY" -w "%{http_code}" -X "$method" "$url" || true
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
}

say "===== nacos console ====="
code="$(request GET "$NACOS_CONSOLE_URL")"
if [ "$code" = "200" ] || [ "$code" = "302" ] || [ "$code" = "301" ]; then
  say "PASS GET $NACOS_CONSOLE_URL -> $code"
else
  say "FAIL GET $NACOS_CONSOLE_URL -> $code"
  fail=1
fi

say "===== health ====="
code="$(request GET "$USER_URL/actuator/health")"
expect "$code" 200 "user-service GET $USER_URL/actuator/health"
say "$(cat "$BODY")"
code="$(request GET "$PRODUCT_URL/actuator/health")"
expect "$code" 200 "product-service GET $PRODUCT_URL/actuator/health"
say "$(cat "$BODY")"
code="$(request GET "$ORDER_URL/actuator/health")"
expect "$code" 200 "order-service GET $ORDER_URL/actuator/health"
say "$(cat "$BODY")"

if [ "$fail" = "0" ]; then
  say "SMOKE TEST PASSED"
  exit 0
fi
say "SMOKE TEST FAILED"
exit 1
