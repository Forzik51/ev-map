#!/usr/bin/env bash
# Copy a built bootJar to this host first; deploy never replaces media or secrets.
set -euo pipefail
artifact=${1:?Usage: release.sh /path/to/boot.jar}
[[ -f $artifact ]] || exit 2
release=/opt/evmap/releases/$(date -u +%Y%m%dT%H%M%SZ).jar
previous=$(readlink -f /opt/evmap/current.jar || true)
install -o root -g root -m 0644 "$artifact" "$release"
ln -sfn "$release" /opt/evmap/current.jar
systemctl restart evmap
for attempt in $(seq 1 90); do
    if curl --fail --silent --max-time 3 http://127.0.0.1:8080/health; then
        echo ' Release is healthy.'
        exit 0
    fi
    sleep 2
done
echo 'Health check failed; inspect journalctl -u evmap.' >&2
if [[ -n $previous && -f $previous ]]; then
    ln -sfn "$previous" /opt/evmap/current.jar
    systemctl restart evmap
    echo 'Previous JAR restored. Database migrations are not reversed.' >&2
fi
exit 1
