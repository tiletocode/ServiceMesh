package com.jwjang.product.infrastructure.persistence

import com.jwjang.product.domain.model.Product
import com.jwjang.product.domain.model.Sku
import com.jwjang.product.domain.repository.ProductRepository
import com.jwjang.product.domain.repository.SkuRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

interface ProductJpaRepository : JpaRepository<Product, Long> {
    fun findAllByIsOnSaleTrue(pageable: Pageable): Page<Product>
}

interface SkuJpaRepository : JpaRepository<Sku, Long> {
    fun findBySkuCode(skuCode: String): Optional<Sku>
    fun findByProductId(productId: Long): List<Sku>
}

@Repository
class ProductRepositoryImpl(private val jpa: ProductJpaRepository) : ProductRepository {
    override fun save(product: Product): Product = jpa.save(product)
    override fun findById(id: Long): Optional<Product> = jpa.findById(id)
    override fun findAll(pageable: Pageable): Page<Product> = jpa.findAll(pageable)
    override fun findAllByIsOnSaleTrue(pageable: Pageable): Page<Product> = jpa.findAllByIsOnSaleTrue(pageable)
}

@Repository
class SkuRepositoryImpl(private val jpa: SkuJpaRepository) : SkuRepository {
    override fun save(sku: Sku): Sku = jpa.save(sku)
    override fun findById(id: Long): Optional<Sku> = jpa.findById(id)
    override fun findBySkuCode(skuCode: String): Optional<Sku> = jpa.findBySkuCode(skuCode)
    override fun findByProductId(productId: Long): List<Sku> = jpa.findByProductId(productId)
}
