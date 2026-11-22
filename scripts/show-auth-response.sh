#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."
BODY="${TMPDIR:-/tmp}/week9-show-body.json"

say() {
  printf '%s\n' "$*"
}

say "FILE scripts/show-auth-response.sh"
say "GET http://127.0.0.1:8080/api/health"
code="$(curl -sS -o "$BODY" -w "%{http_code}" "http://127.0.0.1:8080/api/health")"
say "HTTP ${code}"
tr -d '\r' < "$BODY"
printf '\n\n'

say "GET http://127.0.0.1:8080/api/orders"
code="$(curl -sS -o "$BODY" -w "%{http_code}" "http://127.0.0.1:8080/api/orders")"
say "HTTP ${code}"
tr -d '\r' < "$BODY"
printf '\n\n'

say "POST http://127.0.0.1:8080/api/auth/login"
code="$(curl -sS -o "$BODY" -w "%{http_code}" -X POST "http://127.0.0.1:8080/api/auth/login" -H "Content-Type: application/json" -d '{"username":"buyer","password":"buyer-week09"}')"
say "HTTP ${code}"
python3 -c 'import json,sys; print(json.dumps(json.load(open(sys.argv[1], encoding="utf-8")), ensure_ascii=False, indent=2))' "$BODY"
printf '\n'

token="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], encoding="utf-8"))["token"])' "$BODY")"
say "GET http://127.0.0.1:8080/api/users"
code="$(curl -sS -o "$BODY" -w "%{http_code}" "http://127.0.0.1:8080/api/users" -H "Authorization: Bearer ${token}")"
say "HTTP ${code}"
tr -d '\r' < "$BODY"
printf '\n'
rm -f "$BODY"
