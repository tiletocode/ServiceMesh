package com.jwjang.product

import com.jwjang.product.domain.model.ProductCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

/**
 * product-service 통합 테스트
 * 상품 등록 → SKU 추가 → 재고 차감 → 재고 복구 전체 흐름 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductServiceIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        var savedProductId: Long = 0
        var savedSkuId: Long = 0
    }

    @Test
    @Order(1)
    fun `상품 등록 성공 - 201 반환`() {
        val request = mapOf(
            "name" to "나이키 티셔츠",
            "description" to "편한 면 소재 티셔츠",
            "salesPrice" to 35000,
            "category" to "CLOTHING",
            "imageUrl" to "https://example.com/nike-tshirt.jpg"
        )
        val response = restTemplate.postForEntity(
            "/api/v1/products",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = response.body!!
        assertThat(body["name"]).isEqualTo("나이키 티셔츠")
        assertThat(body["category"]).isEqualTo("CLOTHING")
        assertThat(body["isOnSale"]).isEqualTo(true)
        savedProductId = (body["id"] as Int).toLong()
    }

    @Test
    @Order(2)
    fun `상품 조회 성공`() {
        val response = restTemplate.getForEntity(
            "/api/v1/products/$savedProductId",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["name"]).isEqualTo("나이키 티셔츠")
    }

    @Test
    @Order(3)
    fun `SKU 추가 성공 - 초기 재고 100개`() {
        val request = mapOf(
            "skuCode" to "NIKE-TS-S-BLUE",
            "optionName" to "S / Blue",
            "initialStock" to 100
        )
        val response = restTemplate.postForEntity(
            "/api/v1/products/$savedProductId/skus",
            request,
            Map::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        val body = response.body!!
        assertThat(body["skuCode"]).isEqualTo("NIKE-TS-S-BLUE")
        assertThat(body["stockQuantity"]).isEqualTo(100)
        savedSkuId = (body["id"] as Int).toLong()
    }

    @Test
    @Order(4)
    fun `SKU 재고 조회 성공`() {
        val response = restTemplate.getForEntity(
            "/api/v1/products/skus/$savedSkuId",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body!!["stockQuantity"]).isEqualTo(100)
    }

    @Test
    @Order(5)
    fun `재고 차감 성공 - 30개 차감 후 70개 남음`() {
        val request = mapOf("quantity" to 30)
        val response = restTemplate.postForEntity(
            "/api/v1/products/skus/$savedSkuId/deduct",
            request,
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        // 차감 후 재고 확인
        val stockResponse = restTemplate.getForEntity(
            "/api/v1/products/skus/$savedSkuId",
            Map::class.java
        )
        assertThat(stockResponse.body!!["stockQuantity"]).isEqualTo(70)
    }

    @Test
    @Order(6)
    fun `재고 복구 성공 - 10개 복구 후 80개`() {
        val request = mapOf("quantity" to 10)
        val response = restTemplate.postForEntity(
            "/api/v1/products/skus/$savedSkuId/restore",
            request,
            Unit::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val stockResponse = restTemplate.getForEntity(
            "/api/v1/products/skus/$savedSkuId",
            Map::class.java
        )
        assertThat(stockResponse.body!!["stockQuantity"]).isEqualTo(80)
    }

    @Test
    @Order(7)
    fun `재고 부족 시 오류 발생`() {
        val request = mapOf("quantity" to 9999)
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

        val response = restTemplate.postForEntity(
            "/api/v1/products/skus/$savedSkuId/deduct",
            HttpEntity(request, headers),
            Map::class.java
        )
        // 재고 부족으로 409 CONFLICT 응답
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @Order(8)
    fun `상품 목록 조회 성공`() {
        val response = restTemplate.getForEntity(
            "/api/v1/products",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val content = response.body!!["content"] as List<*>
        assertThat(content).isNotEmpty()
    }

    @Test
    @Order(9)
    fun `존재하지 않는 상품 조회 시 404 반환`() {
        val response = restTemplate.getForEntity(
            "/api/v1/products/99999",
            Map::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
