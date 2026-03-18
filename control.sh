#!/bin/bash
# ================================================================
# MSA 서비스 기동·중지·상태 통합 스크립트
#
# 사용법:
#   ./control.sh --start   # 빌드 없이 기동 (기본, 이미지 재사용)
#   ./control.sh --build   # Gradle 빌드 + Docker 이미지 생성만 (기동 하지 않음)
#   ./control.sh --bst     # 빌드 + 기동 + E2E 테스트 일괄 실행
#   ./control.sh --stop    # 전체 중지 (앱 + 인프라)
#   ./control.sh --down    # 앱만 중지 (인프라 유지)

#   ./control.sh --status  # 상태 확인
#   ./control.sh --test    # 테스트만 실행 (서비스 기동 중일 때)
# ================================================================
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# ── 옵션 파싱 ──────────────────────────────────────────────────
RUN_TEST=false
DO_DOWN=false
DO_STOP=false
DO_BUILD=false
DO_STATUS=false
DO_START=false
for arg in "$@"; do
  case $arg in
    --start)   DO_START=true ;;
    --build)   DO_BUILD=true ;;
    --bst)     DO_BUILD=true; DO_START=true; RUN_TEST=true ;;
    --test)    RUN_TEST=true ;;
    --down)    DO_DOWN=true ;;
    --stop)    DO_STOP=true ;;
    --status)  DO_STATUS=true ;;
  esac
done

# ── 인수 없으면 usage 출력 ─────────────────────────────────────
if [[ $# -eq 0 ]]; then
  echo ""
  echo -e "\033[0;36m사용법: ./control.sh <옵션>\033[0m"
  echo ""
  echo "  --start    빌드 없이 기동 (기본, 이미지 재사용)"
  echo "  --build    Gradle 빌드 + Docker 이미지 생성만 (기동 하지 않음)"
  echo "  --bst      빌드 + 기동 + E2E 테스트 일괄 실행"
  echo "  --test     테스트만 실행 (서비스 기동 중일 때)"
  echo "  --stop     전체 중지 (앱 + 인프라)"
  echo "  --down     앱만 중지 (인프라 유지)"
  echo "  --status   컨테이너 상태 확인"
  echo ""
  exit 0
fi

# ── 알 수 없는 옵션 탐지 ─────────────────────────────────
if ! $DO_START && ! $DO_BUILD && ! $DO_STOP && ! $DO_DOWN && ! $DO_STATUS && ! $RUN_TEST; then
  echo -e "\033[1;33m  ⚠️  알 수 없는 옵션입니다. 인수 없이 실행하면 사용법을 확인할 수 있습니다.\033[0m"
  exit 1
fi

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

# ── --stop: 앱 + 인프라 전체 중지 ─────────────────────────────
if $DO_STOP; then
  info "전체 서비스 중지 중 (앱 + 인프라)..."
  docker compose stop gateway-service member-service product-service order-service payment-service shipping-service
  docker compose stop kafka zookeeper redis postgres
  ok "전체 중지 완료"
  exit 0
fi

# ── --down: 앱만 중지 (인프라 유지) ───────────────────────────
if $DO_DOWN; then
  info "마이크로서비스 종료 중... (인프라는 유지됩니다)"
  docker compose stop gateway-service member-service product-service order-service payment-service shipping-service
  ok "앱 서비스 중지 완료"
  exit 0
fi

# ── --status: 상태 확인 ────────────────────────────────────────
if $DO_STATUS; then
  echo ""
  echo -e "${CYAN}=== 컨테이너 상태 ===${NC}"
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "NAMES|servicemesh"
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

# ── --start·--build·--bst 없이는 기동/빌드 로직 진입 불가 ───────────────
if ! $DO_START && ! $DO_BUILD; then
  # --test 단독: 서비스 기동 없이 테스트만 실행
  if $RUN_TEST; then
    GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
    echo -e "${CYAN}▶ E2E 테스트 실행 (test-api.sh)${NC}"
    chmod +x test-api.sh
    bash test-api.sh
    exit 0
  fi
  exit 0
fi

# ================================================================
echo ""
echo -e "${CYAN}=================================================${NC}"
echo -e "${CYAN}  MSA Shop — 서비스 기동${NC}"
echo -e "${CYAN}=================================================${NC}"
echo ""

# ── STEP 1: 인프라 기동 ────────────────────────────────────────
info "STEP 1/3  인프라 기동 (postgres, redis, zookeeper, kafka)"
docker compose up -d postgres redis zookeeper kafka

echo -ne "   PostgreSQL 대기 중"
until docker compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
  echo -n "."; sleep 2
done
echo -e " ${GREEN}[UP]${NC}"

echo -ne "   Kafka 대기 중"
until docker compose exec -T kafka kafka-topics --bootstrap-server localhost:29092 --list > /dev/null 2>&1; do
  echo -n "."; sleep 3
done
echo -e " ${GREEN}[UP]${NC}"

ok "인프라 준비 완료"
echo ""

# ── STEP 2: 서비스 빌드 or 기동 ────────────────────────────────
if $DO_BUILD; then
  info "STEP 2/3  마이크로서비스 빌드 & 기동 (첫 빌드는 5~10분 소요)"
  docker compose up -d --build \
    member-service product-service order-service payment-service shipping-service gateway-service
else
  info "STEP 2/3  마이크로서비스 기동 (빌드 생략, 기존 이미지 사용)"
  docker compose up -d \
    member-service product-service order-service payment-service shipping-service gateway-service
fi

info "서비스 헬스체크 중..."
wait_health "member-service"   "http://localhost:8081/actuator/health"
wait_health "product-service"  "http://localhost:8082/actuator/health"
wait_health "order-service"    "http://localhost:8083/actuator/health"
wait_health "payment-service"  "http://localhost:8084/actuator/health"
wait_health "shipping-service" "http://localhost:8085/actuator/health"
wait_health "gateway-service"  "http://localhost:8080/actuator/health"
ok "전체 서비스 UP"
echo ""

# ── STEP 3: E2E 테스트 ─────────────────────────────────────────
if $RUN_TEST; then
  info "STEP 3/3  E2E 테스트 실행 (test-api.sh)"
  chmod +x test-api.sh
  bash test-api.sh
else
  echo -e "${YELLOW}💡 빌드 후 재기동:          ./control.sh --build && ./control.sh --start${NC}"
  echo -e "${YELLOW}💡 빌드+기동+테스트 일괄:   ./control.sh --bst${NC}"
  echo -e "${YELLOW}💡 E2E 테스트 실행:         ./control.sh --test${NC}"
  echo -e "${YELLOW}💡 앱만 중지 (인프라 유지): ./control.sh --down${NC}"
  echo -e "${YELLOW}💡 전체 중지 (앱+인프라):   ./control.sh --stop${NC}"
  echo -e "${YELLOW}💡 상태 확인:               ./control.sh --status${NC}"
fi

echo ""
echo -e "${GREEN}=================================================${NC}"
echo -e "${GREEN}  🚀 http://localhost:8080  (프론트엔드 — Spring Cloud Gateway)${NC}"
echo -e "${GREEN}=================================================${NC}"
echo ""
