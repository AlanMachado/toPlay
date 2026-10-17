package br.fidelidade.resgates

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

enum class StatusResgate { CONCLUIDO, CANCELADO }

@Entity
@Table(name = "resgates")
class Resgate(
    @Column(name = "cliente_id") val clienteId: Long,
    @Column(name = "item_id") val itemId: Long,
    val pontos: Long,
    @Enumerated(EnumType.STRING) var status: StatusResgate = StatusResgate.CONCLUIDO,
    @Column(name = "criado_em") val criadoEm: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface ResgateRepository : JpaRepository<Resgate, Long>
