package br.fidelidade.resgates

import br.fidelidade.catalogo.CatalogoRepository
import br.fidelidade.catalogo.Precificacao
import br.fidelidade.clientes.ClienteRepository
import br.fidelidade.comum.ErroNegocio
import br.fidelidade.comum.naoEncontrado
import br.fidelidade.lancamentos.Lancamento
import br.fidelidade.lancamentos.LancamentoRepository
import br.fidelidade.lancamentos.TipoLancamento
import br.fidelidade.parceiros.ParceiroRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ResultadoResgate(val resgateId: Long, val itemId: Long, val pontosUsados: Long, val saldoRestante: Long)

@Service
class ResgateService(
    private val clientes: ClienteRepository,
    private val catalogo: CatalogoRepository,
    private val parceiros: ParceiroRepository,
    private val resgates: ResgateRepository,
    private val lancamentos: LancamentoRepository,
) {
    @Transactional
    fun resgatar(clienteId: Long, itemId: Long): ResultadoResgate {
        // Trava o cliente primeiro: outro resgate do mesmo cliente espera este terminar.
        clientes.travarPorId(clienteId) ?: throw naoEncontrado("Cliente")
        val item = catalogo.findByIdOrNull(itemId)?.takeIf { it.ativo } ?: throw naoEncontrado("Item")

        val parceiro = parceiros.findByIdOrNull(item.parceiroId) ?: throw naoEncontrado("Parceiro")

        // O custo é calculado agora, com o preço do ponto atual do parceiro (ou a promoção vigente).
        val custo = Precificacao.custoEmPontos(item, parceiro).pontos

        val saldo = lancamentos.saldoDe(clienteId)
        if (saldo < custo) {
            throw ErroNegocio("Saldo insuficiente: você tem $saldo pontos e o item custa $custo")
        }

        // Grava os pontos efetivamente cobrados: mudanças futuras de preço não alteram resgates passados.
        val resgate = resgates.save(Resgate(clienteId = clienteId, itemId = itemId, pontos = custo))
        lancamentos.save(
            Lancamento(
                clienteId = clienteId,
                tipo = TipoLancamento.RESGATE,
                pontos = -custo,
                // Valor do item em reais: quanto a plataforma deve repassar ao parceiro.
                valor = item.preco,
                parceiroId = item.parceiroId,
                resgateId = resgate.id,
                descricao = "Resgate: ${item.nome} (${parceiro.nome})",
            ),
        )

        return ResultadoResgate(resgate.id!!, itemId, custo, saldo - custo)
    }
}
