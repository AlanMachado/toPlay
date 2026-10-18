package br.fidelidade.clientes

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

@Entity
@Table(name = "clientes")
class Cliente(
    val nome: String,
    val email: String,
    @Column(name = "criado_em") val criadoEm: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface ClienteRepository : JpaRepository<Cliente, Long> {
    fun findByEmailIgnoreCase(email: String): Cliente?

    /**
     * Trava a linha do cliente até o fim da transação. Usado em operações que gastam
     * pontos, para que dois resgates simultâneos não usem o mesmo saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    fun travarPorId(id: Long): Cliente?
}
