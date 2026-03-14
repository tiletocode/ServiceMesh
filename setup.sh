#!/bin/bash
# ================================================================
# MSA 인프라 systemd 서비스 설치 스크립트 (최초 1회만 실행)
# 사용법: sudo ./setup.sh
# ================================================================
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_NAME="msa-infra"
SERVICE_FILE="$DIR/infra/msa-infra.service"

# root 권한 확인
if [[ $EUID -ne 0 ]]; then
  echo "❌ sudo 로 실행해주세요: sudo ./setup.sh"
  exit 1
fi

REAL_USER="${SUDO_USER:-$USER}"

echo "▶ [1/4] docker 그룹에 $REAL_USER 추가..."
usermod -aG docker "$REAL_USER"
echo "   완료 (재로그인 없이 sg docker 로 활성화됩니다)"

echo "▶ [2/3] systemd 서비스 파일 설치..."
# WorkingDirectory를 실제 경로로 치환
sed "s|/home/hada1/ServiceMesh|$DIR|g" "$SERVICE_FILE" \
  > /etc/systemd/system/${SERVICE_NAME}.service
echo "   → /etc/systemd/system/${SERVICE_NAME}.service"

echo "▶ [3/3] 인프라 지금 바로 시작..."
systemctl daemon-reload
systemctl start "${SERVICE_NAME}.service"
systemctl status "${SERVICE_NAME}.service" --no-pager -l

echo ""
echo "✅ 설치 완료!"
echo ""
echo "  이제부터:"
echo "    ./start.sh          — 서비스만 빌드·기동 (인프라는 이미 실행 중)"
echo "    ./start.sh --test   — 서비스 기동 + E2E 테스트"
echo "    ./start.sh --down   — 서비스 종료 (인프라는 유지)"
echo ""
echo "  인프라 직접 제어:"
echo "    sudo systemctl start   msa-infra"
echo "    sudo systemctl stop    msa-infra"
echo "    sudo systemctl restart msa-infra"
echo "    sudo systemctl status  msa-infra"
