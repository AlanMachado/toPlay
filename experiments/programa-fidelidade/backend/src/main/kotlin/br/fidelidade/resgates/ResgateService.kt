package br.fidelidade.resgates

import br.fidelidade.catalogo.CatalogoRepository
import br.fidelidade.clientes.ClienteRepository
import br.fidelidade.comum.ErroNegocio
import br.fidelidade.comum.naoEncontrado
import br.fidelidade.lancamentos.Lancamento
import br.fidelidade.lancamentos.LancamentoRepository
import br.fidelidade.lancamentos.TipoLancamento
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ResultadoResgate(val resgateId: Long, val itemId: Long, val pontosUsados: Long, val saldoRestante: Long)

@Service
class ResgateService(
    private val clientes: ClienteRepository,
    private val catalogo: CatalogoRepository,
    private val resgates: ResgateRepository,
    private val lancamentos: LancamentoRepository,
) {
    @Transactional
    fun resgatar(clienteId: Long, itemId: Long): ResultadoResgate {
        // Trava o cliente primeiro: outro resgate do mesmo cliente espera este terminar.
        clientes.travarPorId(clienteId) ?: throw naoEncontrado("Cliente")
        val item = catalogo.findByIdOrNull(itemId)?.takeIf { it.ativo } ?: throw naoEncontrado("Item")

        val saldo = lancamentos.saldoDe(clienteId)
        if (saldo < item.custoPontos) {
            throw ErroNegocio("Saldo insuficiente: você tem $saldo pontos e o item custa ${item.custoPontos}")
        }

        val resgate = resgates.save(Resgate(clienteId = clienteId, itemId = itemId, pontos = item.custoPontos))
        lancamentos.save(
            Lancamento(
                clienteId = clienteId,
                tipo = TipoLancamento.RESGATE,
                pontos = -item.custoPontos,
                resgateId = resgate.id,
                descricao = "Resgate: ${item.nome}",
            ),
        )

        return ResultadoResgate(resgate.id!!, itemId, item.custoPontos, saldo - item.custoPontos)
    }
}
