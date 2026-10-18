package br.fidelidade.comum

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Erro de regra de negócio, com uma mensagem que pode ser mostrada ao usuário. */
class ErroNegocio(
    mensagem: String,
    val status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY,
) : RuntimeException(mensagem)

fun naoEncontrado(oQue: String) = ErroNegocio("$oQue não encontrado", HttpStatus.NOT_FOUND)

data class RespostaErro(val erro: String)

@RestControllerAdvice
class TratadorDeErros {
    @ExceptionHandler(ErroNegocio::class)
    fun negocio(e: ErroNegocio) = ResponseEntity.status(e.status).body(RespostaErro(e.message ?: "Erro"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacao(e: MethodArgumentNotValidException): ResponseEntity<RespostaErro> {
        val mensagem = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(RespostaErro(mensagem))
    }
}
