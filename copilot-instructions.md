## 프로젝트 목표
- kubernetes와 istio 환경 학습을 위해 MSA 구조의 어플리케이션이 필요하다. Spring Cloud(Java)을 사용할것이다.
- DDD(Domain-Driven Design) 규칙을 준수할것

## MSA 서비스 리스트
0. 게이트웨이 서비스 (Gateway Service)
책임: 클라이언트의 모든 요청을 받아 적절한 마이크로서비스로 라우팅한다.
주요 기능: Spring Cloud Gateway 기반 동적 라우팅, 정적 콘텐츠(SPA) 서빙, 어드민 리셋 경로 재작성(`/api/v1/admin/reset/{service}` → `/api/v1/admin/reset`).
특징: 기존 nginx 리버스 프록시를 대체한다. Kubernetes 이관 시 Eureka 연동(`lb://` 라우팅)으로 전환 가능하도록 설계되어 있다.
결과: gateway-service (게이트웨이 마이크로서비스, 포트 8080)
1. 회원 서비스 (Member Service)
책임: 고객의 회원 가입, 로그인, 정보 수정, 탈퇴를 관리한다.
주요 기능: JWT(JSON Web Token) 기반의 인증(Authentication)을 처리하며, API 게이트웨이와 연동하여 시스템 전반의 사용자 식별을 책임진다. 
특징: 모든 서비스에서 가장 기본이 되는 데이터(회원 ID, 이름, 등급)를 제공한다.
2. 상품 서비스 (Product Service)
책임: 상품 정보(이름, 가격, 설명)를 관리하고, 가장 중요한 '재고(Inventory)'를 관리한다.
주요 기능: 상품 전시, 상세 정보 조회, 그리고 '주문' 시 재고 차감을 담당한다.
특징: '상품 상세 조회'처럼 읽기(Read) 요청이 압도적으로 많고, '재고 차감'처럼 쓰기(Write) 경쟁이 치열하게 발생한다. CQRS 및 캐싱 패턴을 적용할것이다.
3. 주문 서비스 (Order Service)
책임: 고객의 '장바구니'부터 '주문 생성'까지의 핵심 비즈니스 로직을 담당한다.
주요 기능: 주문을 생성(Create)하고, 주문 내역을 조회(Read)한다.
특징: MSA에서 가장 복잡한 서비스다. 주문을 생성하기 위해 '회원' 정보가 필요하고, '상품'의 재고를 확인해야 하며, '결제'를 요청하고, '배송'을 위한 이벤트를 발행해야 한다. SAGA 패턴을 이용한 분산 트랜잭션의 중심이 된다.
4. 결제 서비스 (Payment Service)
책임: 실제 외부 PG(Payment Gateway)사와 연동하여 결제를 승인하고 취소하는 역할을 담당한다.
주요 기능: '주문' 서비스의 요청을 받아 결제를 시도하고, 그 결과를 '주문' 서비스에 통보한다.
특징: SAGA 패턴에서 '주문'에 대한 보상 트랜잭션(결제 취소)을 수행하는 중요한 역할을 맡는다.
5. 배송 서비스 (Shipping Service)
책임: '주문'이 '결제' 완료되었을 때, 상품 배송을 시작하고 배송 상태(준비 중, 배송 중, 배송 완료)를 추적한다.
주요 기능: Kafka를 통해 '주문 완료' 이벤트를 구독(Subscribe)하여 배송 프로세스를 시작한다.
특징: 주문 서비스와 완벽하게 분리된 비동기(Async) 이벤트 기반 아키텍처(EDA)를 연습하기 위한 핵심 도메인이다.

## 용어집
컨텍스트 (Context)	용어 (Term)	정의 (Definition)	유의어/배제 용어
회원 (Membership)	회원 (Member)	우리 시스템에 가입하여 고유 ID를 발급받은 개인.	사용자(User), 고객(Customer), 계정(Account). (모두 Member로 통일)
회원 등급 (Member Grade)	회원의 활동(예: 구매액)에 따라 부여되는 혜택 수준. (예: BRONZE, SILVER, GOLD)	레벨(Level)
상품/전시 (Catalog)	상품 (Product)	고객에게 '전시'되고 판매되는 대상. 이름, 설명, 대표 이미지, 판매 가격을 갖는다.	아이템(Item)
판매 가격 (Sales Price)	할인이 적용된 실제 고객 결제 기준 가격. (프로모션, 쿠폰 적용 전)	정가(List Price)
재고 (Inventory)	재고 상품 (Stock Keeping Unit, SKU)	재고 관리를 위한 물리적/논리적 최소 단위. 상품(Product)과 1:N 관계일 수 있음. (예: '나이키 티셔츠' 상품의 'S/Blue' SKU)	상품(Product) (재고 컨텍스트에서는 상품 대신 SKU로 명확히 구분)
재고 수량 (Stock Quantity)	주문 가능한 실제 가용 수량.	수량(Quantity) (모호함)
주문 (Ordering)	주문 (Order)	회원이 하나 이상의 주문 항목을 구매하겠다고 요청한 단일 트랜잭션. 주문 번호를 갖는다.	구매(Purchase)
주문 항목 (Order Line)	주문에 포함된 개별 SKU와 수량, 주문 당시의 '주문 가격'을 스냅샷으로 기록한다.	주문 상품(Order Product)
주문 가격 (Order Price)	주문 항목이 생성될 당시의 SKU 판매 가격. (이후 상품의 판매 가격이 변경되어도 불변)	판매 가격(Sales Price)
주문 상태 (Order Status)	주문의 생명주기. (예: PENDING, PAID, SHIPPED, CANCELLED)	
결제 (Payment)	결제 (Payment)	주문에 대한 금액 지불 시도 및 그 결과. 주문과는 1:1 또는 1:N(분할 결제) 관계.	청구(Billing)
배송 (Shipping)	배송 (Shipment)	결제가 완료된 주문을 고객에게 전달하는 물리적 프로세스. 송장 번호를 갖는다.	딜리버리(Delivery)

## 청사진
1. 회원(Membership) 컨텍스트
핵심 책임 (Responsibility): 고객의 '신원(Identity)'을 관리하고 인증(Authentication)을 책임진다.
주요 UL: 회원 (Member), 회원 등급(Member Grade)
주요 기능: 회원 가입, 로그인(인증), 회원 정보 조회 및 수정, 회원 탈퇴.
경계: '회원'의 구매 내역(Order)이나 배송지(Shipping) 정보는 회원 컨텍스트의 핵심 책임이 아니다. 이 컨텍스트는 오직 '신원' 관리에만 집중한다.
결과: member-service (회원 마이크로서비스)
2. 상품(Product) 컨텍스트
핵심 책임 (Responsibility): 판매할 상품의 정보를 '전시'하고, 판매 가능한 '재고'를 관리한다.
주요 UL: 상품(Product), SKU (Stock Keeping Unit), 판매 가격 (Sales Price), 재고 수량(Stock Quantity)
주요 기능: 상품 등록/수정/조회, 카테고리 관리, 재고 관리(입고/차감).
경계: '상품'이 '누구에게' 팔렸는지(Order)는 이 컨텍스트의 관심사가 아니다. 오직 '판매할 대상'과 '판매할 재고'에만 집중한다.
결과: product-service (상품 마이크로서비스)
3. 주문(Ordering) 컨텍스트
핵심 책임 (Responsibility): 고객의 구매 요청(주문)을 생성하고, 그 상태(Lifecycle)를 추적한다.
주요 UL: 주문(Order), 주문 항목(Order Line), 주문 가격(Order Price), 주문 상태(Order Status)
주요 기능: 주문 생성(장바구니 포함), 주문 내역 조회, 주문 취소.
경계: '주문'은 '어떻게' 결제되었는지(Payment) 상세히 알 필요가 없다. 단지 '결제 완료(PAID)'라는 '주문 상태'만 관리한다. 마찬가지로 '어떻게' 배송되는지(Shipping) 알 필요 없이, '배송 시작(SHIPPED)' 상태만 알면 된다.
결과: order-service (주문 마이크로서비스)
4. 결제(Payment) 컨텍스트
핵심 책임 (Responsibility): 주문에 대한 '금액 지불'을 처리하고, 환불을 관리한다.
주요 UL: 결제(Payment)
주요 기능: 외부 PG(결제 대행사) 연동을 통한 결제 시도, 결제 승인/실패 처리, 결제 취소(환불).
경계: '결제'는 어떤 '상품'이 결제되는지(Product) 관심 없다. 오직 주문 컨텍스트로부터 전달받은 주문 번호와 결제할 총액에만 집중한다.
결과: payment-service (결제 마이크로서비스)
5. 배송(Shipping) 컨텍스트
핵심 책임 (Responsibility): '결제가 완료된' 주문을 고객에게 물리적으로 전달하는 프로세스를 관리한다.
주요 UL: 배송(Shipment), 송장 번호(Tracking Number)
주요 기능: '결제 완료' 이벤트 수신, 배송 요청 생성(출고 지시), 송장 번호 발급 및 배송 상태 추적.
경계: '배송'은 주문이 '얼마에' 결제되었는지(Payment) 관심 없다. 오직 '배송할 주문 항목'과 '배송지 주소'에만 집중한다.
결과: shipping-service (배송 마이크로서비스)

---

## 프론트엔드 구현 현황

### 구조
- **단일 파일 SPA**: `member-service/src/main/resources/static/index.html`
  - Bootstrap 5 + Vanilla JS
  - gateway-service(8080)의 catch-all 라우트(`Path=/**` → member-service:8081)를 통해 `/`로 서빙
  - 로그인 후 JWT를 `localStorage`에 저장, 모든 API 요청에 `Authorization: Bearer` 헤더 사용

### 화면 구성 (섹션별)
1. **회원 섹션** – 회원 가입 / 로그인
2. **상품 섹션** – 상품+SKU 목록 조회, 상품 등록, SKU 등록
3. **주문 섹션** – 주문 생성, 주문 목록 (`size=100&sort=id,desc`)
4. **배송 섹션** – 내 배송 목록 전체 조회, 주문별 배송 조회

### API 문서 (springdoc-openapi 자동 생성)
- **Swagger UI 통합**: `http://localhost:8080/swagger-ui.html`
  - gateway-service에서 5개 서비스 API를 하나의 UI로 통합 제공
  - 좌상단 드롭다운으로 서비스 전환 가능
- **동작 방식**:
  - 각 서비스: `springdoc-openapi-starter-webmvc-ui` 의존성 → `/v3/api-docs` 자동 생성
  - gateway-service: `springdoc-openapi-starter-webflux-ui` 의존성 → `/apidocs/{service}/v3/api-docs` 프록시 라우트로 집계
  - 엔드포인트 추가·수정 시 코드 변경만으로 자동 반영 (별도 문서 수정 불필요)
- **각 서비스 개별 api-docs URL** (gateway 통해 접근):
  - `/apidocs/member/v3/api-docs`
  - `/apidocs/product/v3/api-docs`
  - `/apidocs/order/v3/api-docs`
  - `/apidocs/payment/v3/api-docs`
  - `/apidocs/shipping/v3/api-docs`
- **member-service Security 설정**: `/v3/api-docs/**` permitAll 추가 (SecurityConfig.kt)

### 주요 JS 함수
| 함수 | 역할 |
|------|------|
| `login()` | 로그인 후 JWT 저장, 네비게이션 버튼 노출 |
| `loadOrders()` | `GET /api/v1/orders?size=100&sort=id,desc` 호출, 주문 카드 렌더링 |
| `loadShipments()` | `GET /api/v1/shipments/my` 호출, 배송 목록 카드 렌더링 |
| `lookupShipmentByOrder(orderId)` | 주문 카드의 [배송조회] 버튼 → 해당 주문의 배송 정보 표시 |
| `fillShipmentCard(shipment)` | 배송 상태·송장번호·송장사 렌더링 |
| `fillTrackingAndSearch(trackingNumber, carrier)` | 송장번호 표시 및 조회 버튼 활성화 |
| `confirmReset()` | confirm 팝업 후 `resetAll()` 호출 |
| `resetAll()` | shipping→payment→order→product→member 순으로 `POST /api/v1/admin/reset/{service}` 호출 후 자동 로그아웃 |

### 주문 카드 표시 내용
- 주문 ID, 상태 (PENDING/PAID/CANCELLED 배지)
- **취소 사유 (`cancelReason`)**: CANCELLED 상태일 때 빨간 텍스트로 사유 표시
  - 사용자 직접 취소: "사용자 직접 취소"
  - 결제 실패(Kafka 이벤트): `event.failureReason ?: "결제 실패"`
- 주문 항목(SKU명, 수량, 금액)
- [배송조회] / [주문취소] 버튼

### 네비게이션 버튼
- 로그인 전: 로그인/회원가입만 표시
- 로그인 후: 상품/주문/배송/로그아웃/**데이터 초기화(빨간)** 버튼 표시

---

## 인프라 / DevOps 현황

### Docker Compose 구성
- 포트: gateway:8080, member:8081, product:8082, order:8083, payment:8084, shipping:8085
- 총 6개 마이크로서비스 (gateway + 5개 비즈니스 서비스)
- `postgres_data` named volume → 컨테이너 재시작 후에도 DB 데이터 영구 보존
- Kafka + Zookeeper (SAGA 이벤트 브로커)
- Redis (product-service 캐시용)

### Gateway Service (`gateway-service/src/main/resources/application.yml`) 핵심 설정
- Spring Cloud Gateway 기반, 포트 8080
- nginx 리버스 프록시를 완전히 대체
- 어드민 리셋 라우팅: `RewritePath` 필터로 `/api/v1/admin/reset/{service}` → `/api/v1/admin/reset` 재작성
- 정적 콘텐츠 catch-all: `Path=/**` → member-service:8081 (SPA 서빙)
- Kubernetes 이관 시 `EUREKA_CLIENT_ENABLED=true` + `lb://서비스명` 방식으로 전환 가능

### control.sh 사용법
| 옵션 | 설명 |
|------|------|
| `--start` | 빌드 없이 기동 (기본, 기존 이미지 재사용) |
| `--build` | Gradle 빌드 + Docker 이미지 생성만 (기동 하지 않음) |
| `--bst` | 빌드 + 기동 + E2E 테스트 일괄 실행 |
| `--test` | 테스트만 실행 (서비스 기동 중일 때) |
| `--stop` | 앱 + 인프라 전체 중지 |
| `--down` | 앱만 중지 (인프라 유지) |
| `--status` | 컨테이너 상태 확인 |

**`--start` 기동 순서 (3단계)**
1. 인프라 기동: `postgres → redis → zookeeper → kafka` (각각 ready 대기)
2. 서비스 기동: `member / product / order / payment / shipping / gateway-service` (기존 이미지 재사용) → `actuator/health` 헬스체크 대기
3. `--bst` 사용 시에만 `test-api.sh` 자동 실행

**`--build` vs `--start` vs `--bst`**
- 코드 변경 후: `--build` → `--start` 순으로 실행
- 전체 처음부터: `--bst` 한 번으로 빌드·기동·테스트 일괄 처리
- 인프라 재시작 없이 앱 코드만 재배포: `--build` → `--start`

### Redis PageImpl 캐시 이슈 (해결 완료)
- `@Cacheable` 페이지 쿼리에서 제거 (`getOnSaleProducts()`)
- `RedisCacheConfig.kt`에 `@EventListener(ApplicationReadyEvent)` 추가 → 서비스 시작 시 캐시 자동 초기화

---

## AdminResetController (5개 서비스 공통)
- 엔드포인트: `POST /api/v1/admin/reset`
- `deleteAll()` 후 삭제 건수 반환
- member: `MemberJpaRepository`, product: `ProductJpaRepository` + `SkuJpaRepository`, order: `OrderJpaRepository`, payment: `PaymentJpaRepository`, shipping: `ShipmentJpaRepository`
- **주의**: member-service의 `MemberRepository`(커스텀 인터페이스)에는 `deleteAll()` 없음 → `MemberJpaRepository` 직접 주입 필요

---

## 결제 시뮬레이션 규칙
- 주문 금액 **≤ 100만원**: APPROVE → 주문 PAID + 배송 생성
- 주문 금액 **> 100만원**: FAIL → 주문 CANCELLED (취소 사유: Kafka `failureReason`)

---

## 주요 해결 이력
| 문제 | 해결 |
|------|------|
| nginx → Spring Cloud Gateway 교체 | gateway-service 추가, docker-compose에서 nginx 제거, 라우팅 YAML로 관리 |
| start.sh → control.sh 통합 | `--start/--stop/--down/--status` 옵션 체계 통일, usage 출력 추가 |
| 정적 swagger.html → springdoc-openapi | 코드 기반 자동 문서 생성, gateway에서 5개 서비스 통합 (`/swagger-ui.html`) |
| Kafka `NodeExists` 오류 | ZK→Kafka 순서로 docker compose 수동 재시작 |
| Redis `PageImpl` 역직렬화 500 | `@Cacheable` 제거 + 시작 시 캐시 초기화 |
| 주문 목록 10개만 표시 | `?size=100&sort=id,desc` 파라미터 추가 |
| CANCELLED 사유 구별 불가 | `cancelReason` DB 컬럼(V2 Flyway) + 백엔드 + UI |