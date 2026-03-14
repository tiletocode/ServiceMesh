package com.jwjang.order.domain.exception

class OrderNotFoundException(id: Long) :
    RuntimeException("주문을 찾을 수 없습니다. id=$id")

class OrderNotCancellableException(orderNumber: String, status: String) :
    RuntimeException("주문을 취소할 수 없습니다. orderNumber=$orderNumber, 현재 상태=$status")

class OrderAlreadyCancelledException(orderNumber: String) :
    RuntimeException("이미 취소된 주문입니다. orderNumber=$orderNumber")
