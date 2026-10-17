package br.fidelidade.lancamentos

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.Instant

enum class TipoLancamento { CREDITO, RESGATE, ESTORNO, VENCIMENTO }

/**
 * Uma linha do livro-razão de pontos. É imutável: depois de gravado, nunca é alterado.
 * Créditos têm pontos positivos; resgates e vencimentos, negativos.
 */
@Entity
@Table(name = "lancamentos")
class Lancamento(
    @Column(name = "cliente_id") val clienteId: Long,
    @Enumerated(EnumType.STRING) val tipo: TipoLancamento,
    val pontos: Long,
    val valor: BigDecimal? = null,
    @Column(name = "parceiro_id") val parceiroId: Long? = null,
    @Column(name = "resgate_id") val resgateId: Long? = null,
    val descricao: String,
    @Column(name = "criado_em") val criadoEm: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface LancamentoRepository : JpaRepository<Lancamento, Long> {
    /** O saldo nunca é guardado: é sempre a soma dos lançamentos. */
    @Query("select coalesce(sum(l.pontos), 0) from Lancamento l where l.clienteId = :clienteId")
    fun saldoDe(clienteId: Long): Long

    fun findByClienteIdOrderByCriadoEmDescIdDesc(clienteId: Long, pagina: Pageable): List<Lancamento>
}
