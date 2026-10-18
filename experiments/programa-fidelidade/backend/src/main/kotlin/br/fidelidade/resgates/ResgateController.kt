package br.fidelidade.resgates

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class NovoResgate(
    @field:NotNull val clienteId: Long?,
    @field:NotNull val itemId: Long?,
)

@RestController
@RequestMapping("/api/resgates")
class ResgateController(private val resgates: ResgateService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun resgatar(@Valid @RequestBody novo: NovoResgate): ResultadoResgate =
        resgates.resgatar(novo.clienteId!!, novo.itemId!!)
}
