package com.jwjang.order.domain.model

/**
 * 주문 상태 (Value Object - Enum)
 * 주문의 생명주기를 표현
 */
enum class OrderStatus {
    /** 주문 생성 (결제 대기) */
    PENDING,
    /** 결제 완료 */
    PAID,
    /** 배송 시작 */
    SHIPPED,
    /** 배송 완료 */
    DELIVERED,
    /** 주문 취소 */
    CANCELLED
}
