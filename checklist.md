[Eureka]
## 각 서비스에 Eureka client 코드는 작성되어 있으나 Eureka server가 없음
[gateway-service]
## jwt를 localstorage 기반으로 사용해도 동작에 문제가 없나?
  - gateway-service의 dependency에는 현재 jjwt같은 라이브러리가 없다
## API GATEWAY에 timeout, retry 설정이 추가되어 있나
[order-service]
## product-service를 호출하는 Openfeign에 서킷브레이커(Resilience4j) 패턴 적용필요
  - Retry:
    max-attempts: 3
    wait-duration: 100ms
  - Fallback:
    과거 데이터 조회 후 반환
    Fallback 로직을 탈 경우 view에서 '상품 조회 불가: 과거 데이터를 출력합니다' 메시지 출력