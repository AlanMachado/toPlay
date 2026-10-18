package br.loyalty.voucher

import br.loyalty.common.isWithinValidity
import br.loyalty.product.Product
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

/** Modelo de voucher que aparece para o cliente ativar. */
@Entity
@Table(name = "voucher")
class Voucher(
    /** Se preenchido, tem precedência: o voucher só vale para este produto. */
    @Column(name = "product_id") val productId: Long?,
    /** Usado quando não há produto: o voucher vale para qualquer produto deste parceiro. */
    @Column(name = "partner_id") val partnerId: Long?,
    /** Custo para emitir e crédito do voucher, em pontos. */
    @Column(name = "worth_points") val worthPoints: Long,
    val active: Boolean = true,
    @Column(name = "valid_until") val validUntil: Instant? = null,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
) {
    fun isAvailable(now: Instant): Boolean = active && isWithinValidity(validUntil, now)
}

interface VoucherRepository : JpaRepository<Voucher, Long> {
    fun findByActiveTrue(): List<Voucher>
}

/**
 * Voucher ativado por um cliente. Validade, crédito e escopo foram copiados do modelo na emissão,
 * então desativar ou editar o modelo depois não afeta este voucher.
 */
@Entity
@Table(name = "voucher_emitted")
class VoucherEmitted(
    val code: String,
    @Column(name = "voucher_id") val voucherId: Long,
    @Column(name = "client_id") val clientId: Long,
    @Column(name = "worth_points") val worthPoints: Long,
    @Column(name = "product_id") val productId: Long?,
    @Column(name = "partner_id") val partnerId: Long?,
    @Column(name = "valid_until") val validUntil: Instant?,
    @Column(name = "created_at") val createdAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
) {
    fun isWithinValidity(now: Instant): Boolean = isWithinValidity(validUntil, now)

    /** Escopo: o produto do voucher tem precedência; sem produto, vale qualquer produto do parceiro. */
    fun appliesTo(product: Product): Boolean {
        val voucherProductId = productId
        return if (voucherProductId != null) voucherProductId == product.id else partnerId == product.partnerId
    }
}

interface VoucherEmittedRepository : JpaRepository<VoucherEmitted, Long> {
    fun existsByCode(code: String): Boolean

    fun findByClientIdOrderByCreatedAtDesc(clientId: Long): List<VoucherEmitted>

    fun findByCode(code: String): VoucherEmitted?

    /** Trava o voucher até o fim da transação: dois pedidos não consomem o mesmo crédito. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VoucherEmitted v where v.code = :code")
    fun lockByCode(code: String): VoucherEmitted?
}
