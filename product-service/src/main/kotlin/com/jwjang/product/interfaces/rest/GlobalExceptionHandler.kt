package com.jwjang.product.interfaces.rest

import com.jwjang.product.domain.exception.InsufficientStockException
import com.jwjang.product.domain.exception.ProductNotFoundException
import com.jwjang.product.domain.exception.SkuNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException::class)
    fun handleProductNotFound(e: ProductNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(status = 404, error = "NOT_FOUND", message = e.message ?: ""))

    @ExceptionHandler(SkuNotFoundException::class)
    fun handleSkuNotFound(e: SkuNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(status = 404, error = "NOT_FOUND", message = e.message ?: ""))

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(e: InsufficientStockException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(status = 409, error = "CONFLICT", message = e.message ?: ""))

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(status = 409, error = "CONFLICT", message = e.message ?: ""))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException) =
        ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, error = "BAD_REQUEST", message = e.message ?: ""))
}
