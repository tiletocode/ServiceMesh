## 프로젝트 목표
- kubernetes와 istio 환경 학습을 위해 MSA 구조의 어플리케이션이 필요하다. Spring Cloud(Kotlin)을 사용할것이다.
- DDD(Domain-Driven Design) 규칙을 준수할것

## MSA 서비스 리스트
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