#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "=== Deploying ren-finance ==="
docker compose pull 2>/dev/null || true
docker compose build --no-cache backend
docker compose up -d --remove-orphans
echo "=== Checking health ==="
sleep 5
curl -f http://localhost:3000/api/health || {
  echo "Health check failed. Checking logs..."
  docker compose logs --tail=20 backend
  exit 1
}
echo "=== Deploy complete ==="
