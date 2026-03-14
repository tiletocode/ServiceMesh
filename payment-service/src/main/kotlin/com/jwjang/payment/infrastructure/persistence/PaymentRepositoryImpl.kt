package com.jwjang.payment.infrastructure.persistence

import com.jwjang.payment.domain.model.Payment
import com.jwjang.payment.domain.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

interface PaymentJpaRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): Optional<Payment>
    fun findByOrderNumber(orderNumber: String): Optional<Payment>
}

@Repository
class PaymentRepositoryImpl(private val jpa: PaymentJpaRepository) : PaymentRepository {
    override fun save(payment: Payment): Payment = jpa.save(payment)
    override fun findById(id: Long): Optional<Payment> = jpa.findById(id)
    override fun findByOrderId(orderId: Long): Optional<Payment> = jpa.findByOrderId(orderId)
    override fun findByOrderNumber(orderNumber: String): Optional<Payment> = jpa.findByOrderNumber(orderNumber)
}
