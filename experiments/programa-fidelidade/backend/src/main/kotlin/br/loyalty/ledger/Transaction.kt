package br.loyalty.ledger

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

enum class TransactionType {
    /** Compra informada pelo parceiro: gera pontos (positivo ou zero) e pode consumir um voucher. */
    PURCHASE,

    /** Emissão de voucher: debita os pontos do cliente (negativo). */
    VOUCHER_EMISSION,
}

/** Uma linha do livro-razão de pontos. É imutável: depois de gravada, nunca é alterada. */
@Entity
@Table(name = "transaction")
class Transaction(
    @Column(name = "client_id") val clientId: Long,
    @Enumerated(EnumType.STRING) val type: TransactionType,
    @Column(name = "total_points") val totalPoints: Long,
    @Column(name = "product_id") val productId: Long? = null,
    val quantity: Int? = null,
    val price: BigDecimal? = null,
    @Column(name = "voucher_emitted_id") val voucherEmittedId: Long? = null,
    @Column(name = "voucher_points_used") val voucherPointsUsed: Long = 0,
    @Column(name = "partner_id") val partnerId: Long? = null,
    @Column(name = "external_order_id") val externalOrderId: String? = null,
    @Column(name = "created_at") val createdAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
)

interface TransactionRepository : JpaRepository<Transaction, Long> {
    /** O saldo nunca é guardado: é sempre a soma do histórico. */
    @Query("select coalesce(sum(t.totalPoints), 0) from Transaction t where t.clientId = :clientId")
    fun balanceOf(clientId: Long): Long

    /** Quanto de um voucher emitido já foi consumido em compras. */
    @Query("select coalesce(sum(t.voucherPointsUsed), 0) from Transaction t where t.voucherEmittedId = :voucherEmittedId and t.type = br.loyalty.ledger.TransactionType.PURCHASE")
    fun pointsUsedFromVoucher(voucherEmittedId: Long): Long

    fun findByClientIdOrderByCreatedAtDescIdDesc(clientId: Long, page: Pageable): List<Transaction>

    fun findByPartnerIdAndExternalOrderIdOrderById(partnerId: Long, externalOrderId: String): List<Transaction>
}
