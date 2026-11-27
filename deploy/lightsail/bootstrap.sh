#!/usr/bin/env bash
# Run as root on a new Ubuntu 24.04 Lightsail instance.
set -euo pipefail
umask 077
cd "$(dirname "$0")"
export DEBIAN_FRONTEND=noninteractive
export EVMAP_MAIL_FROM=${1:?Usage: bootstrap.sh SENDER_EMAIL}
# apt drops privileges while validating repository signatures.
if [[ -f /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc ]]; then
    chmod 0644 /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
fi
apt-get update -qq
apt-get install -y -qq ca-certificates curl gnupg python3-venv openjdk-21-jre-headless nginx unzip
install -d -m 0755 /usr/share/postgresql-common/pgdg
curl --fail --silent --show-error https://www.postgresql.org/media/keys/ACCC4CF8.asc \
    -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
chmod 0644 /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
printf '%s\n' 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt noble-pgdg main' \
    > /etc/apt/sources.list.d/pgdg.list
apt-get update -qq
apt-get install -y -qq postgresql-17 postgresql-17-postgis-3

if ! swapon --show=NAME --noheadings | grep -qx /swapfile; then
    if [[ ! -e /swapfile ]]; then
        fallocate -l 2G /swapfile
        chmod 0600 /swapfile
        mkswap /swapfile
    fi
    swapon /swapfile
fi
grep -q '^/swapfile ' /etc/fstab || printf '/swapfile none swap sw 0 0\n' >> /etc/fstab
printf 'vm.swappiness=10\n' > /etc/sysctl.d/90-evmap.conf
sysctl -p /etc/sysctl.d/90-evmap.conf

id evmap &>/dev/null || useradd --system --home-dir /opt/evmap --shell /usr/sbin/nologin evmap
install -d -m 0755 /opt/evmap/releases
install -d -m 0750 -o evmap -g evmap /var/lib/evmap/media
install -d -m 0700 /etc/evmap /var/backups/evmap
install -d -m 0755 /var/www/letsencrypt
if [[ ! -f /etc/evmap/app.env ]]; then
    python3 - <<'PY'
import base64, os, secrets
values = {
    'SPRING_PROFILES_ACTIVE': 'prod',
    'SERVER_ADDRESS': '127.0.0.1',
    'SPRING_DATASOURCE_URL': 'jdbc:postgresql://127.0.0.1:5432/evmap?currentSchema=evmap,public',
    'SPRING_DATASOURCE_USERNAME': 'evmap',
    'SPRING_DATASOURCE_PASSWORD': secrets.token_hex(32),
    'SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE': '4',
    'SPRING_DATASOURCE_HIKARI_MINIMUMIDLE': '1',
    'SPRING_DATASOURCE_HIKARI_CONNECTIONTIMEOUT': '5000',
    'SERVER_TOMCAT_THREADS_MAX': '30',
    'SERVER_TOMCAT_THREADS_MIN_SPARE': '2',
    'APP_JWT_SECRET': secrets.token_hex(48),
    'APP_PASSWORD_RESET_SECRET': base64.b64encode(secrets.token_bytes(32)).decode(),
    'APP_MEDIA_ROOT': '/var/lib/evmap/media',
    'MAIL_HOST': 'email-smtp.eu-north-1.amazonaws.com',
    'MAIL_PORT': '587',
    'MAIL_USERNAME': '',
    'MAIL_PASSWORD': '',
    'APP_MAIL_FROM': os.environ['EVMAP_MAIL_FROM'],
}
fd = os.open('/etc/evmap/app.env', os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as f:
    for name, value in values.items():
        f.write(f'{name}={value}\n')
PY
fi
# Password consists only of generated hexadecimal characters.
db_password=$(sed -n 's/^SPRING_DATASOURCE_PASSWORD=//p' /etc/evmap/app.env)
if ! runuser -u postgres -- psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='evmap'" | grep -qx 1; then
    runuser -u postgres -- psql -v ON_ERROR_STOP=1 -c "CREATE ROLE evmap LOGIN PASSWORD '$db_password'"
fi
if ! runuser -u postgres -- psql -tAc "SELECT 1 FROM pg_database WHERE datname='evmap'" | grep -qx 1; then
    runuser -u postgres -- createdb --owner=evmap evmap
fi
runuser -u postgres -- psql -d evmap -v ON_ERROR_STOP=1 <<'SQL'
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
SQL
cat > /etc/postgresql/17/main/conf.d/evmap.conf <<'CONF'
listen_addresses = '127.0.0.1'
max_connections = 20
shared_buffers = '64MB'
effective_cache_size = '256MB'
work_mem = '2MB'
maintenance_work_mem = '32MB'
CONF
chmod 0644 /etc/postgresql/17/main/conf.d/evmap.conf
systemctl restart postgresql
install -m 0644 evmap.service /etc/systemd/system/evmap.service
systemctl daemon-reload
systemctl enable evmap

# Pin a Certbot version that supports IP certificates with webroot validation.
[[ -x /opt/certbot/bin/python ]] || python3 -m venv /opt/certbot
/opt/certbot/bin/pip install --disable-pip-version-check --quiet 'certbot==5.10.0'
cat > /etc/systemd/system/evmap-cert-renew.service <<'UNIT'
[Unit]
Description=Renew Evmap IP TLS certificate
[Service]
Type=oneshot
ExecStart=/opt/certbot/bin/certbot renew --quiet --no-random-sleep-on-renew
TimeoutStartSec=20min
UNIT
cat > /etc/systemd/system/evmap-cert-renew.timer <<'UNIT'
[Unit]
Description=Check Evmap certificate renewal twice daily
[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=1800
Persistent=true
[Install]
WantedBy=timers.target
UNIT
install -d -m 0755 /etc/letsencrypt/renewal-hooks/deploy
printf '#!/bin/sh\n/usr/sbin/nginx -t && /usr/bin/systemctl reload nginx\n' \
    > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx
chmod 0755 /etc/letsencrypt/renewal-hooks/deploy/reload-nginx
systemctl daemon-reload
systemctl enable --now evmap-cert-renew.timer

install -d -m 0755 /etc/systemd/journald.conf.d
printf '[Journal]\nSystemMaxUse=100M\nRuntimeMaxUse=30M\nMaxRetentionSec=7day\n' \
    > /etc/systemd/journald.conf.d/evmap.conf
systemctl restart systemd-journald
printf 'PasswordAuthentication no\nPermitRootLogin no\n' > /etc/ssh/sshd_config.d/40-evmap.conf
sshd -t
systemctl reload ssh
echo 'Bootstrap complete; configure SMTP, TLS and deploy the JAR.'
