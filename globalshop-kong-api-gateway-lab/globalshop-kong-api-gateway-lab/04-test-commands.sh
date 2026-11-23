#!/usr/bin/env bash
# Run from the same machine where kubectl port-forward is listening on localhost:8000.
# Terminal A: kubectl -n globalshop port-forward svc/globalshop-kong 8000:80
set -euo pipefail
BASE="${BASE:-http://localhost:8000}"

echo '1) Homepage (expect 200)'
curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$BASE/"
echo '2) Product API (expect 200)'
curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$BASE/api/products"
echo '3) Partner API without key (expect 401)'
curl -s -o /dev/null -w 'HTTP %{http_code}\n' "$BASE/partner/products"
echo '4) Partner API with key (expect 200 if backend is healthy)'
curl -s -o /dev/null -w 'HTTP %{http_code}\n' -H 'apikey: demo-key-change-me' "$BASE/partner/products"
echo '5) Partner API repeated calls (expect eventually 429 in the same minute)'
for i in 1 2 3 4 5 6; do
  printf 'Request %s: ' "$i"
  curl -s -o /dev/null -w 'HTTP %{http_code}\n' -H 'apikey: demo-key-change-me' "$BASE/partner/products"
done
