package br.loyalty.client

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
@Table(name = "client")
class Client(
    val name: String,
    val email: String,
    @Column(name = "created_at") val createdAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface ClientRepository : JpaRepository<Client, Long> {
    fun findByEmailIgnoreCase(email: String): Client?

    /**
     * Trava a linha do cliente até o fim da transação. Toda operação que mexe no saldo
     * (emitir voucher, registrar pedido) passa por aqui, então duas operações simultâneas
     * do mesmo cliente acontecem uma depois da outra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.id = :id")
    fun lockById(id: Long): Client?
}
