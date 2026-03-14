package com.jwjang.payment.domain.repository

import com.jwjang.payment.domain.model.Payment
import java.util.Optional

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: Long): Optional<Payment>
    fun findByOrderId(orderId: Long): Optional<Payment>
    fun findByOrderNumber(orderNumber: String): Optional<Payment>
}
