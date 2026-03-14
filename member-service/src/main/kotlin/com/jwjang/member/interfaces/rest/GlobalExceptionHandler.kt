package com.jwjang.member.interfaces.rest

import com.jwjang.member.domain.exception.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
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

    @ExceptionHandler(MemberNotFoundException::class)
    fun handleMemberNotFound(e: MemberNotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(status = 404, error = "NOT_FOUND", message = e.message ?: ""))

    @ExceptionHandler(MemberEmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(e: MemberEmailAlreadyExistsException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(status = 409, error = "CONFLICT", message = e.message ?: ""))

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(status = 401, error = "UNAUTHORIZED", message = e.message ?: ""))

    @ExceptionHandler(MemberWithdrawnException::class)
    fun handleMemberWithdrawn(e: MemberWithdrawnException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(status = 403, error = "FORBIDDEN", message = e.message ?: ""))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.allErrors
            .joinToString(", ") { error ->
                if (error is FieldError) "${error.field}: ${error.defaultMessage}"
                else error.defaultMessage ?: ""
            }
        return ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, error = "BAD_REQUEST", message = message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException) =
        ResponseEntity.badRequest()
            .body(ErrorResponse(status = 400, error = "BAD_REQUEST", message = e.message ?: ""))
}
