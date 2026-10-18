package br.fidelidade.parceiros

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "parceiros")
class Parceiro(
    val nome: String,
    /** Quanto o parceiro paga ao programa por ponto emitido. */
    @Column(name = "preco_por_ponto") val precoPorPonto: BigDecimal,
    /** Quantos pontos o cliente ganha por real gasto no parceiro. */
    @Column(name = "pontos_por_real") val pontosPorReal: BigDecimal,
    @Column(name = "criado_em") val criadoEm: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface ParceiroRepository : JpaRepository<Parceiro, Long>
