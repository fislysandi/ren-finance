#!/bin/bash
set -euo pipefail
echo "=== Setting up VPS for ren-finance ==="
apt-get update && apt-get install -y docker.io docker-compose-plugin certbot python3-certbot-nginx
systemctl enable docker && systemctl start docker
mkdir -p /opt/ren-finance/ssl
mkdir -p /opt/ren-finance/backups
mkdir -p /opt/ren-finance/data
echo "=== Setup complete ==="
echo "Next: Copy deployment files to /opt/ren-finance/"
echo "Then: cd /opt/ren-finance && docker compose up -d"
