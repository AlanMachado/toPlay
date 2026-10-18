package br.loyalty.product

import br.loyalty.common.isWithinValidity
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
@Table(name = "product")
class Product(
    val name: String,
    val type: String?,
    @Column(name = "partner_id") val partnerId: Long,
    /** Pontos por unidade de moeda (ex.: 100 = 100 pontos por R$ 1,00). */
    @Column(name = "points_per_currency_unit") val pointsPerCurrencyUnit: BigDecimal,
    val active: Boolean = true,
    @Column(name = "valid_until") val validUntil: Instant? = null,
    @Column(name = "external_id") val externalId: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
) {
    fun isAvailable(now: Instant): Boolean = active && isWithinValidity(validUntil, now)
}

interface ProductRepository : JpaRepository<Product, Long> {
    /** A busca por produto do parceiro é sempre pelo par (parceiro, id externo). */
    fun findByPartnerIdAndExternalId(partnerId: Long, externalId: String): Product?

    fun findByPartnerIdOrderByName(partnerId: Long): List<Product>
}
