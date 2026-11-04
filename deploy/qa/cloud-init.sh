#!/usr/bin/env bash
# EC2 Ubuntu 24.04 ARM64 first-boot script. No credentials are stored in git.
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y openjdk-17-jre-headless caddy curl openssl

install -d -o ubuntu -g ubuntu -m 0750 /opt/pofit-qa
install -d -o ubuntu -g ubuntu -m 0750 /opt/pofit-qa/data
install -d -o ubuntu -g ubuntu -m 0750 /opt/pofit-qa/uploads/imports

if [ ! -e /swapfile ]; then
  fallocate -l 1G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
swapon /swapfile || true

if [ ! -s /opt/pofit-qa/.env-local ]; then
  umask 077
  printf 'JWT_SECRET=%s\nAI_ENABLED=false\nAI_PROVIDER=mock\n' \
    "$(openssl rand -base64 48)" > /opt/pofit-qa/.env-local
  chown ubuntu:ubuntu /opt/pofit-qa/.env-local
  chmod 600 /opt/pofit-qa/.env-local
fi

cat >/etc/systemd/system/pofit-qa.service <<'SERVICE'
[Unit]
Description=POFIT QA Spring Boot
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/pofit-qa
Environment=SERVER_ADDRESS=127.0.0.1
Environment=SPRING_H2_CONSOLE_ENABLED=false
Environment=LOGGING_LEVEL_COM_HONGIK_GRADUATE=INFO
ExecStart=/usr/bin/java -Xms64m -Xmx384m -XX:+UseSerialGC -jar /opt/pofit-qa/app.jar
Restart=on-failure
RestartSec=8
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
SERVICE

cat >/usr/local/sbin/pofit-refresh-domain <<'SCRIPT'
#!/usr/bin/env bash
set -euo pipefail
TOKEN=$(curl --fail --silent --show-error --max-time 4 \
  -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' \
  http://169.254.169.254/latest/api/token)
IP=$(curl --fail --silent --show-error --max-time 4 \
  -H "X-aws-ec2-metadata-token: $TOKEN" \
  http://169.254.169.254/latest/meta-data/public-ipv4)
HOST="$(printf '%s' "$IP" | tr '.' '-').sslip.io"
cat >/etc/caddy/Caddyfile <<CADDY
$HOST {
  encode gzip
  @blocked path /h2-console /h2-console/*
  respond @blocked 404
  reverse_proxy 127.0.0.1:8080
}
CADDY
printf '%s\n' "$HOST" > /opt/pofit-qa/public-host.txt
systemctl reload caddy || true
SCRIPT
chmod 700 /usr/local/sbin/pofit-refresh-domain

cat >/etc/systemd/system/pofit-refresh-domain.service <<'SERVICE'
[Unit]
Description=Refresh POFIT QA free DNS on EC2 restart
Wants=network-online.target
After=network-online.target
Before=caddy.service

[Service]
Type=oneshot
ExecStart=/usr/local/sbin/pofit-refresh-domain

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable pofit-qa
systemctl enable pofit-refresh-domain
/usr/local/sbin/pofit-refresh-domain
systemctl enable --now caddy
systemctl restart caddy
