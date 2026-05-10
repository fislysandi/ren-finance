#!/bin/bash
set -euo pipefail
DOMAIN="${REN_DOMAIN:-ren-finance.example.com}"
EMAIL="${REN_EMAIL:-admin@example.com}"
echo "=== Renewing SSL certificate for $DOMAIN ==="
certbot certonly --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" || \
  certbot certonly --standalone -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" --preferred-challenges http || {
  echo "SSL renewal failed. Check domain DNS and port 80/443 accessibility."
  exit 1
}
# Copy certs to docker volume location
cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /opt/ren-finance/ssl/
cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /opt/ren-finance/ssl/
echo "=== SSL renewed. Restarting Nginx ==="
cd /opt/ren-finance/deployment && docker compose restart nginx
echo "=== Renewal complete ==="
