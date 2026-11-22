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

GATEWAY_PORT="$(read_env GATEWAY_PORT "${GATEWAY_PORT:-}")"
USER_PORT="$(read_env USER_PORT "${USER_PORT:-}")"
GATEWAY_PORT="${GATEWAY_PORT:-8080}"
USER_PORT="${USER_PORT:-8081}"
GATEWAY_URL="http://127.0.0.1:${GATEWAY_PORT}"
USER_URL="http://127.0.0.1:${USER_PORT}"
BODY="${TMPDIR:-/tmp}/week9-auth-body.json"
fail=0

finish() {
  rm -f "$BODY"
}
trap finish EXIT

say() {
  printf '%s\n' "$*" >&2
}

request() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  local token="${4:-}"
  local forged="${5:-}"
  local args=(-sS -o "$BODY" -w "%{http_code}" -X "$method" "$url")
  if [ -n "$data" ]; then
    args+=(-H "Content-Type: application/json" --data "$data")
  fi
  if [ -n "$token" ]; then
    args+=(-H "Authorization: Bearer ${token}")
  fi
  if [ -n "$forged" ]; then
    args+=(-H "X-User-Role: ${forged}" -H "X-User-Name: admin" -H "X-User-Id: 1")
  fi
  curl "${args[@]}"
}

expect() {
  local actual="$1"
  local wanted="$2"
  local label="$3"
  if [ "$actual" = "$wanted" ]; then
    say "PASS $label -> $actual"
  else
    say "FAIL $label -> $actual $(tr -d '\r\n' < "$BODY")"
    fail=1
  fi
}

json_field() {
  python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8")).get(sys.argv[2], ""))' "$1" "$2"
}

login() {
  local username="$1"
  local password="$2"
  local code
  code="$(request POST "$GATEWAY_URL/api/auth/login" "{\"username\":\"${username}\",\"password\":\"${password}\"}")"
  if [ "$code" != "200" ]; then
    say "FAIL login ${username} -> ${code} $(tr -d '\r\n' < "$BODY")"
    fail=1
    printf '%s' ""
    return
  fi
  local role
  role="$(json_field "$BODY" role)"
  say "PASS login ${username} -> ${code} role=${role} token=present"
  json_field "$BODY" token
}

say "GATEWAY $GATEWAY_URL"
say "USER $USER_URL"

health="$(request GET "$GATEWAY_URL/api/health")"
expect "$health" "200" "gateway /api/health"
actuator="$(request GET "$GATEWAY_URL/actuator/health")"
expect "$actuator" "200" "gateway /actuator/health"
products="$(request GET "$GATEWAY_URL/api/products")"
expect "$products" "200" "route product-service GET /api/products"

orders="$(request GET "$GATEWAY_URL/api/orders")"
expect "$orders" "401" "unauthenticated GET /api/orders"

buyer_token="$(login buyer buyer-week09)"
if [ -n "$buyer_token" ]; then
  orders="$(request GET "$GATEWAY_URL/api/orders" "" "$buyer_token")"
  expect "$orders" "200" "buyer token GET /api/orders"
  users="$(request GET "$GATEWAY_URL/api/users" "" "$buyer_token")"
  expect "$users" "403" "buyer token GET /api/users"
  forged="$(request GET "$GATEWAY_URL/api/users" "" "$buyer_token" "ADMIN")"
  expect "$forged" "403" "forged X-User-Role ignored"
fi

direct="$(request GET "$USER_URL/api/users" "" "" "ADMIN")"
expect "$direct" "401" "direct user-service forged header"

admin_token="$(login admin admin-week09)"
if [ -n "$admin_token" ]; then
  users="$(request GET "$GATEWAY_URL/api/users" "" "$admin_token")"
  expect "$users" "200" "admin token GET /api/users"
fi

if [ "$fail" = "0" ]; then
  say "AUTH TEST PASSED"
  exit 0
fi
say "AUTH TEST FAILED"
exit 1
