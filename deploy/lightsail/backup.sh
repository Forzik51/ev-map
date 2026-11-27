#!/usr/bin/env bash
# The bucket must be private and attached to this instance.
set -euo pipefail
umask 077
source /etc/evmap/backup.env
exec 9>/run/evmap-backup.lock
flock -n 9 || exit 0
name=$(date -u +%Y%m%dT%H%M%SZ)
work=$(mktemp -d /var/backups/evmap/.work-XXXXXXXX)
archive=/var/backups/evmap/$name.tar.gz
restart=0
cleanup() {
    if (( restart )); then systemctl start evmap; fi
    rm -rf -- "$work"
}
trap cleanup EXIT
if systemctl is-active --quiet evmap; then
    restart=1
    systemctl stop evmap
fi
runuser -u postgres -- pg_dump -Fc evmap > "$work/database.dump"
tar -czf "$archive" -C "$work" database.dump \
    -C / var/lib/evmap/media
if (( restart )); then
    systemctl start evmap
    restart=0
fi
aws s3 cp "$archive" "s3://$EVMAP_BACKUP_BUCKET/daily/$name.tar.gz" \
    --region eu-north-1 --sse AES256 --only-show-errors
python3 /usr/local/lib/evmap/prune-backups.py "$EVMAP_BACKUP_BUCKET"
find /var/backups/evmap -maxdepth 1 -type f -name '*.tar.gz' -mtime +2 -delete
echo "Backup uploaded: daily/$name.tar.gz"
