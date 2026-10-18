package br.loyalty.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Erro de regra de negócio. As mensagens podem ser mostradas ao usuário. */
class BusinessException(
    val reasons: List<String>,
    val status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
) : RuntimeException(reasons.joinToString("; ")) {
    constructor(reason: String, status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY) : this(listOf(reason), status)
}

fun notFound(what: String) = BusinessException("$what não encontrado", HttpStatus.NOT_FOUND)

/** Corpo de erro padrão: "error" é um resumo, "reasons" traz todos os motivos. */
data class ErrorResponse(val error: String, val reasons: List<String>)

@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(BusinessException::class)
    fun business(e: BusinessException) =
        ResponseEntity.status(e.status).body(ErrorResponse(e.reasons.first(), e.reasons))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val reasons = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(reasons.firstOrNull() ?: "Requisição inválida", reasons))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable(e: HttpMessageNotReadableException) =
        ResponseEntity.badRequest().body(ErrorResponse("JSON inválido ou campo obrigatório ausente", listOf(e.mostSpecificCause.message ?: "")))
}
