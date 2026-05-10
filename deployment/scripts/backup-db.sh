#!/bin/bash
set -euo pipefail
BACKUP_DIR="/opt/ren-finance/backups"
DATA_DIR="/opt/ren-finance/data"
DATE=$(date +%Y-%m-%d)
BACKUP_PATH="$BACKUP_DIR/datahike-$DATE"
echo "=== Backing up Datahike DB ==="
mkdir -p "$BACKUP_PATH"
rsync -av --delete "$DATA_DIR/" "$BACKUP_PATH/"
# Rotate: keep last 7 daily
find "$BACKUP_DIR" -name "datahike-*" -mtime +7 -exec rm -rf {} \; 2>/dev/null || true
echo "=== Backup complete: $BACKUP_PATH ==="
