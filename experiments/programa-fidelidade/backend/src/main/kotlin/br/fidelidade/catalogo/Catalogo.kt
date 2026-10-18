package br.fidelidade.catalogo

import br.fidelidade.parceiros.Parceiro
import br.fidelidade.parceiros.ParceiroRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode

@Entity
@Table(name = "catalogo")
class ItemCatalogo(
    val nome: String,
    val descricao: String?,
    @Column(name = "parceiro_id") val parceiroId: Long,
    /** Preço do item em reais. É a base para calcular o custo em pontos. */
    val preco: BigDecimal,
    /** Custo fixo em pontos, só quando a plataforma faz uma promoção. Nulo = custo calculado. */
    @Column(name = "pontos_promocionais") val pontosPromocionais: Long? = null,
    val ativo: Boolean = true,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface CatalogoRepository : JpaRepository<ItemCatalogo, Long> {
    fun findByAtivoTrue(): List<ItemCatalogo>
}

/** Quanto um item custa em pontos no momento do resgate. */
data class CustoEmPontos(
    /** Pontos que o cliente paga agora (promocional, se houver). */
    val pontos: Long,
    /** Custo calculado pelo preço do ponto do parceiro, sem promoção. */
    val pontosSemPromocao: Long,
) {
    val emPromocao: Boolean get() = pontos < pontosSemPromocao
}

object Precificacao {
    /**
     * Custo = preço do item ÷ preço do ponto do parceiro, arredondado para CIMA
     * (o cliente nunca paga menos pontos do que o item vale).
     *
     * Ex.: item de R$ 25,00 num parceiro que paga R$ 0,025 por ponto -> 1.000 pontos.
     */
    fun custoEmPontos(item: ItemCatalogo, parceiro: Parceiro): CustoEmPontos {
        require(item.parceiroId == parceiro.id) { "O item ${item.id} não pertence ao parceiro ${parceiro.id}" }
        val calculado = item.preco.divide(parceiro.precoPorPonto, 0, RoundingMode.CEILING).longValueExact()
        return CustoEmPontos(pontos = item.pontosPromocionais ?: calculado, pontosSemPromocao = calculado)
    }
}

data class ItemCatalogoDto(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val parceiroId: Long,
    val parceiroNome: String,
    val preco: BigDecimal,
    val custoPontos: Long,
    val custoPontosSemPromocao: Long,
    val emPromocao: Boolean,
)

@RestController
@RequestMapping("/api/catalogo")
class CatalogoController(
    private val catalogo: CatalogoRepository,
    private val parceiros: ParceiroRepository,
) {
    @GetMapping
    fun listar(): List<ItemCatalogoDto> {
        val itens = catalogo.findByAtivoTrue()
        val parceirosPorId = parceiros.findAllById(itens.map { it.parceiroId }.toSet()).associateBy { it.id!! }
        return itens
            .map { item ->
                val parceiro = parceirosPorId.getValue(item.parceiroId)
                val custo = Precificacao.custoEmPontos(item, parceiro)
                ItemCatalogoDto(
                    id = item.id!!,
                    nome = item.nome,
                    descricao = item.descricao,
                    parceiroId = item.parceiroId,
                    parceiroNome = parceiro.nome,
                    preco = item.preco,
                    custoPontos = custo.pontos,
                    custoPontosSemPromocao = custo.pontosSemPromocao,
                    emPromocao = custo.emPromocao,
                )
            }
            .sortedBy { it.custoPontos }
    }
}
