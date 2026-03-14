package com.jwjang.product.domain.repository

import com.jwjang.product.domain.model.Product
import com.jwjang.product.domain.model.Sku
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Optional<Product>
    fun findAll(pageable: Pageable): Page<Product>
    fun findAllByIsOnSaleTrue(pageable: Pageable): Page<Product>
}

interface SkuRepository {
    fun save(sku: Sku): Sku
    fun findById(id: Long): Optional<Sku>
    fun findBySkuCode(skuCode: String): Optional<Sku>
    fun findByProductId(productId: Long): List<Sku>
}
