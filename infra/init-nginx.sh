#!/bin/bash

# ==========================================
# 사용자 설정 변수
# ==========================================
DOMAIN="api.byeolbolil.xyz"  # 백엔드 도메인 (예: api.stargazer.com)
EMAIL="swstar21c@gmail.com"   # SSL 인증서 발급용 이메일
APP_PORT=8080                 # 스프링부트(도커) 포트
# ==========================================

echo "🚀 Nginx 및 Certbot 설치를 시작합니다..."

# 패키지 업데이트 및 설치
sudo apt-get update
sudo apt-get install -y nginx certbot python3-certbot-nginx

# Nginx 설정 파일 작성 (리버스 프록시)
# 8080포트로 떠있는 도커 컨테이너로 트래픽을 넘겨주는 설정
echo "🔧 Nginx 설정 파일을 작성 중입니다..."

sudo tee /etc/nginx/sites-available/$DOMAIN > /dev/null <<EOF
server {
    server_name $DOMAIN;

    location / {
        proxy_pass http://localhost:$APP_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

# 심볼릭 링크 생성 (sites-enabled)
sudo ln -s /etc/nginx/sites-available/$DOMAIN /etc/nginx/sites-enabled/
sudo unlink /etc/nginx/sites-enabled/default  # 기본 설정 제거

# Nginx 설정 테스트 및 재시작
sudo nginx -t
sudo systemctl reload nginx

# SSL 인증서 발급 (Let's Encrypt)
# --non-interactive: 사용자 입력 없이 진행
# --redirect: http 접속 시 https로 자동 리다이렉트
echo "🔒 SSL 인증서를 발급받습니다..."
sudo certbot --nginx -d $DOMAIN --non-interactive --agree-tos -m $EMAIL --redirect

echo "✅ 모든 설정이 완료되었습니다!"
echo "👉 https://$DOMAIN 으로 접속 테스트를 해보세요."
