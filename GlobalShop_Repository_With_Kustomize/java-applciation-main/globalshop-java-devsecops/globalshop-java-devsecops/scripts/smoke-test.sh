#!/usr/bin/env bash
set -euo pipefail
URL="${1:-http://localhost:8080}"
echo "[1/3] Health endpoint"
curl --fail --silent --show-error "$URL/actuator/health"
echo
echo "[2/3] Products API (check 12 seeded products on a fresh DB)"
curl --fail --silent --show-error "$URL/api/products" | python3 -c 'import sys,json; p=json.load(sys.stdin); print("Products:",len(p)); print("First:",p[0]["name"] if p else "none"); assert len(p)>0'
echo "[3/3] Customer UI"
curl --fail --silent --show-error -o /dev/null "$URL/"
echo "Smoke checks passed. Visit $URL and $URL/admin.html"
