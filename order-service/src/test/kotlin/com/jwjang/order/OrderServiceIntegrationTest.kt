package com.jwjang.order

import com.jwjang.order.domain.model.OrderStatus
import com.jwjang.order.domain.repository.OrderRepository
import com.jwjang.order.infrastructure.client.ProductServiceClient
import com.jwjang.order.infrastructure.client.SkuResponse
import com.jwjang.order.infrastructure.client.StockRequest
import com.jwjang.order.infrastructure.event.OrderEventListener
import com.jwjang.order.infrastructure.event.PaymentCompletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * order-service 통합 테스트
 * SAGA: 주문 생성 → 결제 완료 이벤트 처리 → 상태 변경 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = ["order.created", "order.payment.requested", "order.payment.completed", "order.cancelled"]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DirtiesContext
class OrderServiceIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderEventListener: OrderEventListener

    @MockBean
    lateinit var productServiceClient: ProductServiceClient

    companion object {
        var savedOrderId: Long = 0
    }

    @Test
    @Order(1)
    fun `주문 생성 성공 - 재고 차감 및 PENDING 상태 설정`() {
        // ProductServiceClient Mock 설정
        val skuResponse = SkuResponse(
            id = 1L,
            skuCode = "NIKE-TS-S-BLUE",
            optionName = "S / Blue",
            stockQuantity = 100,
            salesPrice = BigDecimal("35000"),
            productName = "나이키 티셔츠"
        )
        given(productServiceClient.getSku(eq(1L))).willReturn(skuResponse)
        given(productServiceClient.deductStock(eq(1L), any())).willReturn(ResponseEntity.ok().build())

        // 주문 생성 요청
        val request = mapOf(
            "memberId" to 100L,
            "shippingAddress" to "서울시 강남구 테스트로 1",
            "orderLines" to listOf(
                mapOf("skuId" to 1L, "quantity" to 2)
            )
        )
        val response = restTemplate.postForEntity("/api/v1/orders", request, Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = response.body!!
        assertThat(body["status"]).isEqualTo("PENDING")
        assertThat(body["memberId"]).isEqualTo(100)
        assertThat(body["totalAmount"]).isIn("70000", "70000.00", 70000, 70000.00)
        savedOrderId = (body["id"] as Int).toLong()

        // Feign 클라이언트가 실제로 호출됐는지 검증
        verify(productServiceClient).getSku(1L)
        verify(productServiceClient).deductStock(eq(1L), any())
    }

    @Test
    @Order(2)
    fun `주문 조회 성공`() {
        val response = restTemplate.getForEntity("/api/v1/orders/$savedOrderId", Map::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["status"]).isEqualTo("PENDING")
    }

    @Test
    @Order(3)
    fun `SAGA - 결제 완료 이벤트 처리 후 주문 상태 PAID로 변경`() {
        val event = PaymentCompletedEvent(
            orderId = savedOrderId,
            orderNumber = "TEST-ORDER",
            paymentId = 1L,
            success = true
        )
        // Kafka 이벤트 리스너 직접 호출 (통합 테스트 단순화)
        orderEventListener.handlePaymentCompleted(event)

        // DB 상태 검증
        val order = orderRepository.findById(savedOrderId).orElseThrow()
        assertThat(order.status).isEqualTo(OrderStatus.PAID)
    }

    @Test
    @Order(4)
    fun `SAGA - 결제 실패 이벤트 처리 후 주문 상태 CANCELLED로 변경`() {
        // 두번째 주문 생성
        val skuResponse = SkuResponse(
            id = 2L, skuCode = "TEST-SKU-02",
            optionName = "M / Red", stockQuantity = 50,
            salesPrice = BigDecimal("50000"), productName = "테스트상품"
        )
        given(productServiceClient.getSku(eq(2L))).willReturn(skuResponse)
        given(productServiceClient.deductStock(eq(2L), any())).willReturn(ResponseEntity.ok().build())
        given(productServiceClient.restoreStock(eq(2L), any())).willReturn(ResponseEntity.ok().build())

        val request = mapOf(
            "memberId" to 101L,
            "shippingAddress" to "서울시 서초구 SAGA로 99",
            "orderLines" to listOf(mapOf("skuId" to 2L, "quantity" to 1))
        )
        val createResponse = restTemplate.postForEntity("/api/v1/orders", request, Map::class.java)
        val failOrderId = (createResponse.body!!["id"] as Int).toLong()

        // 결제 실패 이벤트
        val failEvent = PaymentCompletedEvent(
            orderId = failOrderId,
            orderNumber = "FAIL-ORDER",
            paymentId = 999L,
            success = false,
            failureReason = "카드 한도 초과"
        )
        orderEventListener.handlePaymentCompleted(failEvent)

        val order = orderRepository.findById(failOrderId).orElseThrow()
        assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    @Order(5)
    fun `주문 취소 성공`() {
        // 새 주문 생성
        val skuResponse = SkuResponse(
            id = 3L, skuCode = "CANCEL-SKU",
            optionName = "L / Black", stockQuantity = 30,
            salesPrice = BigDecimal("25000"), productName = "취소테스트상품"
        )
        given(productServiceClient.getSku(eq(3L))).willReturn(skuResponse)
        given(productServiceClient.deductStock(eq(3L), any())).willReturn(ResponseEntity.ok().build())
        given(productServiceClient.restoreStock(eq(3L), any())).willReturn(ResponseEntity.ok().build())

        val request = mapOf(
            "memberId" to 200L,
            "shippingAddress" to "취소주소",
            "orderLines" to listOf(mapOf("skuId" to 3L, "quantity" to 1))
        )
        val createResponse = restTemplate.postForEntity("/api/v1/orders", request, Map::class.java)
        val cancelOrderId = (createResponse.body!!["id"] as Int).toLong()

        // 취소 요청
        val cancelResponse = restTemplate.postForEntity(
            "/api/v1/orders/$cancelOrderId/cancel",
            null,
            Map::class.java
        )
        assertThat(cancelResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(cancelResponse.body!!["status"]).isEqualTo("CANCELLED")

        // 재고 복구 호출 검증
        verify(productServiceClient).restoreStock(eq(3L), any())
    }
}
