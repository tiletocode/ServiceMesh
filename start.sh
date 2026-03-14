#!/bin/bash
# ================================================================
# MSA 서비스 전체 기동 + E2E 테스트 자동 실행 스크립트
# 사용법: ./start.sh [--test] [--down]
# ================================================================
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# ── 옵션 파싱 ──────────────────────────────────────────────────
RUN_TEST=false
DO_DOWN=false
DO_RESTART=false
for arg in "$@"; do
  case $arg in
    --test)    RUN_TEST=true ;;
    --down)    DO_DOWN=true ;;
    --restart) DO_RESTART=true ;;
  esac
done

# ── 색상 출력 ──────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
ok()   { echo -e "${GREEN}  ✅ $1${NC}"; }
info() { echo -e "${CYAN}▶ $1${NC}"; }
warn() { echo -e "${YELLOW}  ⚠️  $1${NC}"; }
fail() { echo -e "${RED}  ❌ $1${NC}"; exit 1; }

# ── docker 그룹 권한 자동 적용 ─────────────────────────────────
if ! docker info &>/dev/null 2>&1; then
  warn "docker 권한 없음 → docker 그룹으로 재실행합니다..."
  exec sg docker "$0" "$@"
fi

# ── --down: 서비스만 종료 (인프라는 유지) ──────────────────────
if $DO_DOWN; then
  info "마이크로서비스 종료 중... (인프라는 유지됩니다)"
  docker compose stop member-service product-service order-service payment-service shipping-service
  ok "서비스 종료 완료"
  exit 0
fi

# ── 포트 대기 함수 ─────────────────────────────────────────────
wait_port() {
  local name=$1 host=$2 port=$3 max=${4:-60}
  echo -ne "   $name 대기 중"
  for i in $(seq 1 $max); do
    if nc -z "$host" "$port" 2>/dev/null; then
      echo -e " ${GREEN}[UP]${NC}"
      return 0
    fi
    echo -n "."
    sleep 2
  done
  echo ""
  fail "$name (:$port) 시작 타임아웃 — docker compose logs 로 확인하세요"
}

# ── 헬스체크 대기 함수 ──────────────────────────────────────────
wait_health() {
  local name=$1 url=$2 max=${3:-120}
  echo -ne "   $name 대기 중"
  for i in $(seq 1 $max); do
    if curl -sf "$url" 2>/dev/null | grep -q '"status":"UP"'; then
      echo -e " ${GREEN}[UP]${NC}"
      return 0
    fi
    echo -n "."
    sleep 3
  done
  echo ""
  fail "$name 헬스체크 타임아웃 — docker compose logs $name 으로 확인하세요"
}

# ── --restart: Zookeeper NodeExists 문제 없이 안전 재시작 ──────
if $DO_RESTART; then
  info "서비스 안전 재시작 중 (Zookeeper → Kafka → 앱 순서)..."
  docker compose stop kafka order-service payment-service shipping-service member-service product-service
  docker compose stop zookeeper
  docker compose start zookeeper
  sleep 8
  docker compose start kafka
  sleep 10
  docker compose start member-service product-service order-service payment-service shipping-service
  info "헬스체크 중..."
  wait_health "member-service"   "http://localhost:8081/actuator/health"
  wait_health "order-service"    "http://localhost:8083/actuator/health"
  docker compose restart nginx
  ok "재시작 완료"
  exit 0
fi

# ================================================================
echo ""
echo -e "${CYAN}=================================================${NC}"
echo -e "${CYAN}  MSA Shop — 서비스 기동 스크립트${NC}"
echo -e "${CYAN}=================================================${NC}"
echo ""

# ── STEP 1: 인프라 상태 확인 ───────────────────────────────────
info "STEP 1/3  인프라 상태 확인"
INFRA_OK=true
for check in "PostgreSQL:localhost:5432" "Redis:localhost:6379" "Kafka:localhost:9093"; do
  name=${check%%:*}; addr=${check#*:}; host=${addr%%:*}; port=${addr##*:}
  if nc -z "$host" "$port" 2>/dev/null; then
    echo -e "   ${GREEN}✅ $name (:$port) UP${NC}"
  else
    echo -e "   ${RED}❌ $name (:$port) DOWN${NC}"
    INFRA_OK=false
  fi
done

if ! $INFRA_OK; then
  warn "인프라가 실행되지 않았습니다."
  warn "systemd 서비스가 설치된 경우: sudo systemctl start msa-infra"
  warn "처음 설치라면 먼저 실행하세요:  sudo ./setup.sh"
  fail "인프라 확인 후 다시 실행해주세요."
fi
ok "인프라 준비 완료"
echo ""

# ── STEP 2: 서비스 빌드 & 기동 ─────────────────────────────────
info "STEP 2/3  마이크로서비스 빌드 & 기동 (첫 빌드는 5~10분 소요)"
docker compose up -d --build \
  member-service product-service order-service payment-service shipping-service

info "서비스 헬스체크 중..."
wait_health "member-service"   "http://localhost:8081/actuator/health"
wait_health "product-service"  "http://localhost:8082/actuator/health"
wait_health "order-service"    "http://localhost:8083/actuator/health"
wait_health "payment-service"  "http://localhost:8084/actuator/health"
wait_health "shipping-service" "http://localhost:8085/actuator/health"
ok "전체 서비스 UP"
echo ""

# ── STEP 3: E2E 테스트 ─────────────────────────────────────────
if $RUN_TEST; then
  info "STEP 3/3  E2E 테스트 실행 (test-api.sh)"
  chmod +x test-api.sh
  bash test-api.sh
else
  echo -e "${YELLOW}💡 E2E 테스트를 실행하려면:  ./start.sh --test${NC}"
  echo -e "${YELLOW}💡 안전 재시작하려면:         ./start.sh --restart${NC}"
  echo -e "${YELLOW}💡 전체 종료하려면:           ./start.sh --down${NC}"
fi

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}  🚀 http://localhost:8081  (프론트엔드)${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
