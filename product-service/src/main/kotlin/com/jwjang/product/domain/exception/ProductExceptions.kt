package com.jwjang.product.domain.exception

class ProductNotFoundException(id: Long) :
    RuntimeException("상품을 찾을 수 없습니다. id=$id")

class SkuNotFoundException(id: Long) :
    RuntimeException("SKU를 찾을 수 없습니다. id=$id")

class InsufficientStockException(skuId: Long, requested: Int, available: Int) :
    RuntimeException("재고가 부족합니다. skuId=$skuId, 요청=$requested, 현재=$available")
