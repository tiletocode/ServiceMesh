#!/bin/bash
# ================================================================
# MSA 초기 설치 스크립트 (최초 1회만 실행)
# 사용법: sudo ./setup.sh
# ================================================================
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"

# root 권한 확인
if [[ $EUID -ne 0 ]]; then
  echo "❌ sudo 로 실행해주세요: sudo ./setup.sh"
  exit 1
fi

REAL_USER="${SUDO_USER:-$USER}"

echo "▶ [1/1] docker 그룹에 $REAL_USER 추가..."
usermod -aG docker "$REAL_USER"
echo "   완료 (재로그인 없이 sg docker 로 활성화됩니다)"

echo ""
echo "✅ 설치 완료!"
echo ""
echo "  이제부터 start.sh 로 모든 기동·중지를 관리합니다:"
echo "    ./start.sh              — 인프라 + 서비스 빌드·기동"
echo "    ./start.sh --no-build   — 빌드 없이 기동 (이미지 재사용)"
echo "    ./start.sh --stop       — 전체 중지 (앱 + 인프라)"
echo "    ./start.sh --down       — 앱만 중지 (인프라 유지)"
echo "    ./start.sh --restart    — Kafka 안전 재시작"
echo "    ./start.sh --status     — 상태 확인"
echo "    ./start.sh --test       — 기동 + E2E 테스트"
