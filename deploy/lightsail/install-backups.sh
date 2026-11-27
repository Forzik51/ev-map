#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
bucket=${1:?Usage: install-backups.sh LIGHTSAIL_BUCKET}
[[ $bucket =~ ^[a-z0-9-]+$ ]] || exit 2
if ! command -v aws >/dev/null; then
    temp=$(mktemp -d)
    trap 'rm -rf -- "$temp"' EXIT
    curl --fail --silent --show-error https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o "$temp/aws.zip"
    unzip -q "$temp/aws.zip" -d "$temp"
    "$temp/aws/install"
fi
install -m 0700 backup.sh /usr/local/sbin/evmap-backup
install -d -m 0755 /usr/local/lib/evmap
install -m 0644 prune-backups.py /usr/local/lib/evmap/prune-backups.py
printf 'EVMAP_BACKUP_BUCKET=%s\n' "$bucket" > /etc/evmap/backup.env
chmod 0600 /etc/evmap/backup.env
install -m 0644 evmap-backup.service evmap-backup.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now evmap-backup.timer
