package br.fidelidade.parceiros

import br.fidelidade.clientes.ClienteRepository
import br.fidelidade.comum.Dinheiro
import br.fidelidade.comum.ErroNegocio
import br.fidelidade.comum.ehPositivo
import br.fidelidade.comum.naoEncontrado
import br.fidelidade.lancamentos.Lancamento
import br.fidelidade.lancamentos.LancamentoRepository
import br.fidelidade.lancamentos.TipoLancamento
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

data class ResultadoCompra(
    val clienteId: Long,
    val parceiroId: Long,
    val valorCompra: BigDecimal,
    val pontosGanhos: Long,
    /** Quanto o parceiro deve ao programa pelos pontos emitidos. */
    val valorDevidoPeloParceiro: BigDecimal,
    val lancamentoId: Long,
)

@Service
class CompraService(
    private val parceiros: ParceiroRepository,
    private val clientes: ClienteRepository,
    private val lancamentos: LancamentoRepository,
) {
    /**
     * O cliente comprou no parceiro: calcula os pontos ganhos e registra o crédito.
     *
     * Exemplo: compra de R$ 100,00 num parceiro que dá 2 pontos por real e paga R$ 0,03 por ponto
     * -> cliente ganha 200 pontos e o parceiro deve R$ 6,00 ao programa.
     */
    @Transactional
    fun registrar(parceiroId: Long, clienteId: Long, valorCompra: BigDecimal): ResultadoCompra {
        if (!valorCompra.ehPositivo()) throw ErroNegocio("O valor da compra deve ser maior que zero")
        if (valorCompra.stripTrailingZeros().scale() > 2) throw ErroNegocio("O valor da compra tem mais de 2 casas decimais")

        val parceiro = parceiros.findByIdOrNull(parceiroId) ?: throw naoEncontrado("Parceiro")
        if (!clientes.existsById(clienteId)) throw naoEncontrado("Cliente")

        // Pontos são inteiros: a fração é descartada (arredonda para baixo, a favor do programa).
        val pontos = (valorCompra * parceiro.pontosPorReal).setScale(0, RoundingMode.DOWN).longValueExact()
        if (pontos <= 0) throw ErroNegocio("Esta compra não gera pontos")

        val valorDevido = Dinheiro.emReais(BigDecimal.valueOf(pontos) * parceiro.precoPorPonto)

        val lancamento = lancamentos.save(
            Lancamento(
                clienteId = clienteId,
                tipo = TipoLancamento.CREDITO,
                pontos = pontos,
                valor = valorDevido,
                parceiroId = parceiroId,
                descricao = "Compra de R$ ${Dinheiro.emReais(valorCompra).toPlainString().replace('.', ',')} em ${parceiro.nome}",
            ),
        )

        return ResultadoCompra(clienteId, parceiroId, valorCompra, pontos, valorDevido, lancamento.id!!)
    }
}
