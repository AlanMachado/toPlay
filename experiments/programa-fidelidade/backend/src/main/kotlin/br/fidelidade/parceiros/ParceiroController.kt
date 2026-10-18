package br.fidelidade.parceiros

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class ParceiroDto(val id: Long, val nome: String, val precoPorPonto: BigDecimal, val pontosPorReal: BigDecimal)

data class NovaCompra(
    @field:NotNull val clienteId: Long?,
    /** Envie como texto no JSON, ex.: "149.90". */
    @field:NotNull val valorCompra: BigDecimal?,
)

@RestController
@RequestMapping("/api/parceiros")
class ParceiroController(
    private val parceiros: ParceiroRepository,
    private val compras: CompraService,
) {
    @GetMapping
    fun listar(): List<ParceiroDto> =
        parceiros.findAll().sortedBy { it.nome }.map { ParceiroDto(it.id!!, it.nome, it.precoPorPonto, it.pontosPorReal) }

    @PostMapping("/{id}/compras")
    @ResponseStatus(HttpStatus.CREATED)
    fun registrarCompra(@PathVariable id: Long, @Valid @RequestBody compra: NovaCompra): ResultadoCompra =
        compras.registrar(id, compra.clienteId!!, compra.valorCompra!!)
}
