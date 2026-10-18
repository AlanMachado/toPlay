package br.loyalty.voucher

import br.loyalty.client.ClientRepository
import br.loyalty.common.BusinessException
import br.loyalty.common.notFound
import br.loyalty.ledger.Transaction
import br.loyalty.ledger.TransactionRepository
import br.loyalty.ledger.TransactionType
import br.loyalty.partner.PartnerRepository
import br.loyalty.product.ProductRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Para exibir a quem um voucher se aplica. */
data class VoucherScope(val productId: Long?, val productName: String?, val partnerId: Long?, val partnerName: String?)

data class AvailableVoucherDto(
    val id: Long,
    val worthPoints: Long,
    val validUntil: Instant?,
    val scope: VoucherScope,
    /** O cliente tem saldo suficiente para emitir. */
    val affordable: Boolean,
)

enum class EmittedVoucherStatus { ACTIVE, USED, EXPIRED }

data class EmittedVoucherDto(
    val id: Long,
    val code: String,
    val worthPoints: Long,
    val pointsUsed: Long,
    val remainingPoints: Long,
    val validUntil: Instant?,
    val scope: VoucherScope,
    val status: EmittedVoucherStatus,
    val createdAt: Instant,
)

@Service
class VoucherService(
    private val vouchers: VoucherRepository,
    private val emitted: VoucherEmittedRepository,
    private val clients: ClientRepository,
    private val transactions: TransactionRepository,
    private val products: ProductRepository,
    private val partners: PartnerRepository,
    private val codes: VoucherCodeGenerator,
) {
    @Transactional(readOnly = true)
    fun availableFor(clientId: Long): List<AvailableVoucherDto> {
        if (!clients.existsById(clientId)) throw notFound("Cliente")
        val balance = transactions.balanceOf(clientId)
        val now = Instant.now()
        return vouchers.findByActiveTrue()
            .filter { it.isAvailable(now) }
            .sortedBy { it.worthPoints }
            .map { AvailableVoucherDto(it.id!!, it.worthPoints, it.validUntil, scopeOf(it.productId, it.partnerId), balance >= it.worthPoints) }
    }

    /**
     * Emite um voucher para o cliente: debita os pontos e gera um código.
     * Só é possível se o saldo (calculado pelo histórico) cobrir o valor do voucher.
     */
    @Transactional
    fun emit(clientId: Long, voucherId: Long): EmittedVoucherDto {
        clients.lockById(clientId) ?: throw notFound("Cliente")
        val voucher = vouchers.findByIdOrNull(voucherId) ?: throw notFound("Voucher")
        if (!voucher.isAvailable(Instant.now())) throw BusinessException("Este voucher não está mais disponível")

        val balance = transactions.balanceOf(clientId)
        if (balance < voucher.worthPoints) {
            throw BusinessException("Saldo insuficiente: você tem $balance pontos e o voucher custa ${voucher.worthPoints}")
        }

        val created = emitted.save(
            VoucherEmitted(
                code = codes.newUniqueCode(),
                voucherId = voucherId,
                clientId = clientId,
                worthPoints = voucher.worthPoints,
                productId = voucher.productId,
                partnerId = voucher.partnerId,
                validUntil = voucher.validUntil,
            ),
        )
        transactions.save(
            Transaction(
                clientId = clientId,
                type = TransactionType.VOUCHER_EMISSION,
                totalPoints = -voucher.worthPoints,
                voucherEmittedId = created.id,
            ),
        )
        return toDto(created, pointsUsed = 0)
    }

    @Transactional(readOnly = true)
    fun emittedFor(clientId: Long): List<EmittedVoucherDto> {
        if (!clients.existsById(clientId)) throw notFound("Cliente")
        return emitted.findByClientIdOrderByCreatedAtDesc(clientId)
            .map { toDto(it, transactions.pointsUsedFromVoucher(it.id!!)) }
    }

    fun remainingPoints(voucher: VoucherEmitted): Long = voucher.worthPoints - transactions.pointsUsedFromVoucher(voucher.id!!)

    private fun toDto(voucher: VoucherEmitted, pointsUsed: Long): EmittedVoucherDto {
        val remaining = voucher.worthPoints - pointsUsed
        val status = when {
            remaining <= 0 -> EmittedVoucherStatus.USED
            !voucher.isWithinValidity(Instant.now()) -> EmittedVoucherStatus.EXPIRED
            else -> EmittedVoucherStatus.ACTIVE
        }
        return EmittedVoucherDto(
            id = voucher.id!!,
            code = voucher.code,
            worthPoints = voucher.worthPoints,
            pointsUsed = pointsUsed,
            remainingPoints = remaining,
            validUntil = voucher.validUntil,
            scope = scopeOf(voucher.productId, voucher.partnerId),
            status = status,
            createdAt = voucher.createdAt,
        )
    }

    private fun scopeOf(productId: Long?, partnerId: Long?): VoucherScope {
        val product = productId?.let { products.findByIdOrNull(it) }
        // Com produto, o parceiro exibido é o dono do produto.
        val effectivePartnerId = product?.partnerId ?: partnerId
        val partner = effectivePartnerId?.let { partners.findByIdOrNull(it) }
        return VoucherScope(product?.id, product?.name, partner?.id, partner?.name)
    }
}
