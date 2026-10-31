package com.romafrihet.server.common.exception

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/** 예외 → 응답 변환은 이 한 곳에서만 한다. */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ProblemDetail {
        logger.warn("BusinessException: {} {}", e.errorCode.code, e.message)
        return e.errorCode.toProblemDetail()
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val fieldError = e.bindingResult.fieldErrors.firstOrNull()
        val errorCode = fieldError?.let { errorCodeOf(it) } ?: ErrorCode.INVALID_VALUE
        return fieldError
            ?.let { errorCode.toProblemDetail("${it.field}: ${it.defaultMessage}") }
            ?: errorCode.toProblemDetail()
    }

    /** 커스텀 검증 애노테이션의 errorCode 속성을 꺼낸다. */
    private fun errorCodeOf(fieldError: FieldError): ErrorCode? {
        if (!fieldError.contains(ConstraintViolation::class.java)) return null
        val attributes = fieldError.unwrap(ConstraintViolation::class.java).constraintDescriptor.attributes
        return attributes["errorCode"] as? ErrorCode
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ProblemDetail {
        val violation = e.constraintViolations.firstOrNull()
        val errorCode =
            violation?.constraintDescriptor?.attributes?.get("errorCode") as? ErrorCode ?: ErrorCode.INVALID_VALUE
        return violation
            ?.let { errorCode.toProblemDetail("${it.propertyPath}: ${it.message}") }
            ?: errorCode.toProblemDetail()
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(): ProblemDetail = ErrorCode.INVALID_REQUEST_BODY.toProblemDetail()

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(): ProblemDetail = ErrorCode.METHOD_NOT_ALLOWED.toProblemDetail()

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(): ProblemDetail = ErrorCode.NOT_FOUND.toProblemDetail()

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ProblemDetail {
        logger.error("Unhandled exception", e)
        return ErrorCode.INTERNAL_SERVER_ERROR.toProblemDetail()
    }
}
