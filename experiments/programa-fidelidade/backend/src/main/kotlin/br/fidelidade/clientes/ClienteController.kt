package br.fidelidade.clientes

import br.fidelidade.comum.ErroNegocio
import br.fidelidade.comum.naoEncontrado
import br.fidelidade.lancamentos.LancamentoRepository
import br.fidelidade.lancamentos.TipoLancamento
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

data class NovoCliente(
    @field:NotBlank val nome: String,
    @field:NotBlank @field:Email val email: String,
)

data class ClienteDto(val id: Long, val nome: String, val email: String)

data class SaldoDto(val clienteId: Long, val pontos: Long)

data class LancamentoDto(
    val id: Long,
    val tipo: TipoLancamento,
    val pontos: Long,
    val valor: BigDecimal?,
    val descricao: String,
    val criadoEm: Instant,
)

private fun Cliente.paraDto() = ClienteDto(id!!, nome, email)

@RestController
@RequestMapping("/api/clientes")
class ClienteController(
    private val clientes: ClienteRepository,
    private val lancamentos: LancamentoRepository,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastrar(@Valid @RequestBody novo: NovoCliente): ClienteDto {
        val email = novo.email.trim().lowercase()
        if (clientes.findByEmailIgnoreCase(email) != null) {
            throw ErroNegocio("Já existe um cliente com este e-mail", HttpStatus.CONFLICT)
        }
        return clientes.save(Cliente(nome = novo.nome.trim(), email = email)).paraDto()
    }

    /** "Login" provisório por e-mail. Ainda não há senha nem autenticação de verdade. */
    @GetMapping
    fun buscarPorEmail(@RequestParam email: String): ClienteDto =
        (clientes.findByEmailIgnoreCase(email.trim()) ?: throw naoEncontrado("Cliente")).paraDto()

    @GetMapping("/{id}/saldo")
    fun saldo(@PathVariable id: Long): SaldoDto {
        garantirQueExiste(id)
        return SaldoDto(id, lancamentos.saldoDe(id))
    }

    @GetMapping("/{id}/extrato")
    fun extrato(@PathVariable id: Long): List<LancamentoDto> {
        garantirQueExiste(id)
        return lancamentos.findByClienteIdOrderByCriadoEmDescIdDesc(id, PageRequest.of(0, 50))
            .map { LancamentoDto(it.id!!, it.tipo, it.pontos, it.valor, it.descricao, it.criadoEm) }
    }

    private fun garantirQueExiste(id: Long) {
        clientes.findByIdOrNull(id) ?: throw naoEncontrado("Cliente")
    }
}
