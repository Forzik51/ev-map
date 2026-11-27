#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
ip=${1:?Usage: configure-tls.sh STATIC_IPV4 EMAIL}
email=${2:?Supply the certificate contact email}
[[ $ip =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]] || exit 2
rm -f /etc/nginx/sites-enabled/default
cat > /etc/nginx/sites-available/evmap <<'NGINX'
server {
    listen 80 default_server;
    location /.well-known/acme-challenge/ {
        root /var/www/letsencrypt;
        try_files $uri =404;
    }
    location / { return 503; }
}
NGINX
ln -sfn /etc/nginx/sites-available/evmap /etc/nginx/sites-enabled/evmap
nginx -t
systemctl reload nginx
/opt/certbot/bin/certbot certonly --non-interactive --agree-tos --email "$email" \
    --cert-name evmap --preferred-profile shortlived --webroot \
    --webroot-path /var/www/letsencrypt --ip-address "$ip"
sed "s/__IP__/$ip/g" nginx.conf.template > /etc/nginx/sites-available/evmap
nginx -t
systemctl reload nginx
