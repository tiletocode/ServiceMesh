#!/bin/bash
# MSA 서비스 end-to-end 테스트 스크립트

BASE_MEMBER="http://localhost:8081/api/v1"
BASE_PRODUCT="http://localhost:8082/api/v1"
BASE_ORDER="http://localhost:8083/api/v1"
BASE_PAYMENT="http://localhost:8084/api/v1"
BASE_SHIPPING="http://localhost:8085/api/v1"

echo "==========================================="
echo "  MSA 서비스 E2E 테스트"
echo "==========================================="

# 1. 회원 가입 (이미 존재하면 무시)
echo ""
echo "[1] 회원 가입..."
REGISTER=$(curl -s -X POST "$BASE_MEMBER/members/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"e2e_test@example.com","password":"Test1234!","name":"E2E테스터"}')
echo "   응답: $REGISTER"

# 2. 로그인
echo ""
echo "[2] 로그인..."
LOGIN=$(curl -s -X POST "$BASE_MEMBER/members/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"e2e_test@example.com","password":"Test1234!"}')
TOKEN=$(echo $LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
MEMBER_ID=$(echo $LOGIN | python3 -c "import sys,json; print(json.load(sys.stdin)['memberId'])")
echo "   memberId: $MEMBER_ID"
echo "   token: ${TOKEN:0:40}..."

# 3. 상품 등록
echo ""
echo "[3] 상품 등록..."
PRODUCT=$(curl -s -X POST "$BASE_PRODUCT/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"갤럭시 노트북","description":"삼성 갤럭시 북4","salesPrice":1500000,"category":"ELECTRONICS"}')
PRODUCT_ID=$(echo $PRODUCT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "   productId: $PRODUCT_ID"

# 4. SKU 추가 (재고 50개)
echo ""
echo "[4] SKU 추가 (재고 50개)..."
SKU=$(curl -s -X POST "$BASE_PRODUCT/products/$PRODUCT_ID/skus" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"skuCode\":\"GALAXY-NB-001\",\"optionName\":\"실버/16GB\",\"initialStock\":50}")
SKU_ID=$(echo $SKU | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "   skuId: $SKU_ID"

# 5. SKU 재고 조회
echo ""
echo "[5] SKU 재고 조회..."
STOCK=$(curl -s "$BASE_PRODUCT/products/skus/$SKU_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "   재고: $(echo $STOCK | python3 -c "import sys,json; print(json.load(sys.stdin)['stockQuantity'])")"

# 6. 주문 생성 (order-service → product-service Feign 호출로 재고 차감)
echo ""
echo "[6] 주문 생성..."
ORDER=$(curl -s -X POST "$BASE_ORDER/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"memberId\":$MEMBER_ID,\"shippingAddress\":\"서울시 강남구 테헤란로 123\",\"orderLines\":[{\"skuId\":$SKU_ID,\"quantity\":2}]}")
ORDER_ID=$(echo $ORDER | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
ORDER_NUMBER=$(echo $ORDER | python3 -c "import sys,json; print(json.load(sys.stdin)['orderNumber'])")
ORDER_STATUS=$(echo $ORDER | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])")
echo "   orderId: $ORDER_ID"
echo "   orderNumber: $ORDER_NUMBER"
echo "   status: $ORDER_STATUS  (PENDING이어야 함)"

# 7. 주문 후 재고 확인 (2개 차감 → 48개)
echo ""
echo "[7] 주문 후 재고 확인..."
STOCK2=$(curl -s "$BASE_PRODUCT/products/skus/$SKU_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "   재고: $(echo $STOCK2 | python3 -c "import sys,json; print(json.load(sys.stdin)['stockQuantity'])") (2개 차감 후)"

# 8. Actuator Health 전체 확인
echo ""
echo "[8] 전체 서비스 Health 확인..."
for name_port in "member:8081" "product:8082" "order:8083" "payment:8084" "shipping:8085"; do
  name=${name_port%%:*}
  port=${name_port##*:}
  health=$(curl -s "http://localhost:$port/actuator/health" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null || echo "DOWN")
  echo "   $name-service (:$port) → $health"
done

echo ""
echo "==========================================="
echo "  ✅ 테스트 완료!"
echo "==========================================="
