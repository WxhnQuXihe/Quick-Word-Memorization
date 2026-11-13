#!/usr/bin/env bash
# Shorthand API smoke test. Requires a running backend with qx/123456 and at least one published book.
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:8081}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-123456}"
USER="${USER_NAME:-qx}"
PASS="${USER_PASS:-123456}"

jqget() { python -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }

echo "[1] health"
curl -fsS "$BASE/actuator/health" | grep -q UP && echo "  ok"

echo "[2] admin login"
ADMIN_TOKEN=$(curl -fsS -X POST "$BASE/api/admin/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | jqget "d['data']['token']")
echo "  ok"

echo "[3] user login"
USER_TOKEN=$(curl -fsS -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" | jqget "d['data']['token']")
echo "  ok"

echo "[4] wordbooks"
BOOKS=$(curl -fsS "$BASE/api/wordbooks" -H "Authorization: Bearer $USER_TOKEN")
BOOK_ID=$(printf '%s' "$BOOKS" | jqget "d['data'][0]['id']")
echo "  book=$BOOK_ID"

echo "[5] select + current group"
curl -fsS -X POST "$BASE/api/wordbooks/$BOOK_ID/select" -H "Authorization: Bearer $USER_TOKEN" >/dev/null
GROUP=$(curl -fsS "$BASE/api/study/current?wordbookId=$BOOK_ID" -H "Authorization: Bearer $USER_TOKEN")
GROUP_ID=$(printf '%s' "$GROUP" | jqget "d['data']['groupId']")
WORD_ID=$(printf '%s' "$GROUP" | jqget "d['data']['words'][0]['id']")
echo "  group=$GROUP_ID word=$WORD_ID"

echo "[6] favorite + known"
curl -fsS -X POST "$BASE/api/study/group/$GROUP_ID/favorite/$WORD_ID" -H "Authorization: Bearer $USER_TOKEN" | grep -q '"data":true' && echo "  favorite ok"
curl -fsS -X POST "$BASE/api/study/group/$GROUP_ID/known/$WORD_ID" -H "Authorization: Bearer $USER_TOKEN" >/dev/null && echo "  known ok"

echo "[7] favorites list"
curl -fsS "$BASE/api/favorites" -H "Authorization: Bearer $USER_TOKEN" | grep -q '"total"' && echo "  ok"

echo "[8] next group"
curl -fsS -X POST "$BASE/api/study/group/$GROUP_ID/next" -H "Authorization: Bearer $USER_TOKEN" | grep -q '"groupId"' && echo "  ok"

echo "SMOKE OK"
