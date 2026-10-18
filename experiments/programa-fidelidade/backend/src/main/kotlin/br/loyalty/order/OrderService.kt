package br.loyalty.order

import br.loyalty.client.Client
import br.loyalty.client.ClientRepository
import br.loyalty.common.BusinessException
import br.loyalty.common.Money
import br.loyalty.common.notFound
import br.loyalty.ledger.Transaction
import br.loyalty.ledger.TransactionRepository
import br.loyalty.ledger.TransactionType
import br.loyalty.partner.Partner
import br.loyalty.partner.PartnerRepository
import br.loyalty.product.Product
import br.loyalty.product.ProductRepository
import br.loyalty.voucher.VoucherEmitted
import br.loyalty.voucher.VoucherEmittedRepository
import br.loyalty.voucher.VoucherService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class OrderItemInput(val productExternalId: String, val quantity: Int?, val price: BigDecimal)

data class ItemResult(
    val productExternalId: String,
    val productId: Long,
    val productName: String,
    val quantity: Int?,
    val price: BigDecimal,
    val voucherPointsUsed: Long,
    /** Parte do preço coberta pelo voucher. Nulo em pedidos repetidos (não é gravado). */
    val coveredByVoucher: BigDecimal?,
    /** Parte do preço que gerou pontos. Nulo em pedidos repetidos (não é gravado). */
    val paid: BigDecimal?,
    val pointsEarned: Long,
)

data class VoucherUsage(
    val code: String,
    val remainingBefore: Long,
    val pointsUsed: Long,
    val remainingAfter: Long,
) {
    val fullyUsed: Boolean get() = remainingAfter <= 0
}

/** Resultado do cálculo de um pedido, sem gravar nada. */
data class OrderEvaluation(
    val reasons: List<String>,
    val items: List<ItemResult>,
    val voucher: VoucherUsage?,
    /** Voucher encontrado (para gravar a transação). */
    val voucherEmitted: VoucherEmitted?,
) {
    val valid: Boolean get() = reasons.isEmpty()
}

data class OrderResponse(
    val externalOrderId: String,
    /** true quando o pedido já tinha sido registrado: nada novo foi gravado. */
    val duplicate: Boolean,
    val items: List<ItemResult>,
    val voucher: VoucherUsage?,
    val totalPointsEarned: Long,
    val clientBalance: Long,
)

data class VoucherCheckResponse(
    val valid: Boolean,
    val reasons: List<String>,
    val voucher: VoucherUsage?,
    val items: List<ItemResult>,
)

@Service
class OrderService(
    private val partners: PartnerRepository,
    private val products: ProductRepository,
    private val clients: ClientRepository,
    private val vouchersEmitted: VoucherEmittedRepository,
    private val voucherService: VoucherService,
    private val transactions: TransactionRepository,
) {
    /** Simula o pedido com o voucher, sem gravar nada. Usado pelo parceiro antes de enviar a compra. */
    @Transactional(readOnly = true)
    fun checkVoucher(partnerId: Long, clientEmail: String, voucherCode: String, items: List<OrderItemInput>): VoucherCheckResponse {
        val partner = findPartner(partnerId)
        val client = findRegisteredClient(clientEmail)
        val evaluation = evaluate(partner, client, voucherCode, items, lockVoucher = false)
        return VoucherCheckResponse(evaluation.valid, evaluation.reasons, evaluation.voucher, evaluation.items)
    }

    /**
     * O parceiro informa um pedido. Cada item vira uma transação de compra no livro-razão.
     *
     * - Cliente não cadastrado: nada é gravado.
     * - Pedido repetido (mesmo parceiro + externalOrderId): devolve o que já foi gravado.
     * - Qualquer item ou voucher inválido: o pedido inteiro é recusado (tudo ou nada).
     */
    @Transactional
    fun register(partnerId: Long, externalOrderId: String, clientEmail: String, voucherCode: String?, items: List<OrderItemInput>): OrderResponse {
        val partner = findPartner(partnerId)
        val client = findRegisteredClient(clientEmail)
        val clientId = client.id!!
        // A trava do cliente também serializa reenvios simultâneos do mesmo pedido.
        clients.lockById(clientId)

        val existing = transactions.findByPartnerIdAndExternalOrderIdOrderById(partnerId, externalOrderId)
        if (existing.isNotEmpty()) return duplicateResponse(externalOrderId, clientId, existing)

        val evaluation = evaluate(partner, client, voucherCode, items, lockVoucher = true)
        if (!evaluation.valid) throw BusinessException(evaluation.reasons)

        val voucherId = evaluation.voucherEmitted?.id
        evaluation.items.forEach { item ->
            transactions.save(
                Transaction(
                    clientId = clientId,
                    type = TransactionType.PURCHASE,
                    totalPoints = item.pointsEarned,
                    productId = item.productId,
                    quantity = item.quantity,
                    price = item.price,
                    voucherEmittedId = if (item.voucherPointsUsed > 0) voucherId else null,
                    voucherPointsUsed = item.voucherPointsUsed,
                    partnerId = partnerId,
                    externalOrderId = externalOrderId,
                ),
            )
        }

        return OrderResponse(
            externalOrderId = externalOrderId,
            duplicate = false,
            items = evaluation.items,
            voucher = evaluation.voucher,
            totalPointsEarned = evaluation.items.sumOf { it.pointsEarned },
            clientBalance = transactions.balanceOf(clientId),
        )
    }

    private fun evaluate(
        partner: Partner,
        client: Client,
        voucherCode: String?,
        items: List<OrderItemInput>,
        lockVoucher: Boolean,
    ): OrderEvaluation {
        val now = Instant.now()
        val reasons = mutableListOf<String>()

        if (!partner.active) reasons += "Parceiro ${partner.name} está inativo"
        if (items.isEmpty()) reasons += "O pedido não tem itens"

        // 1. Produtos: sempre buscados pelo par (parceiro, id externo).
        val resolved = items.mapIndexedNotNull { index, item ->
            val label = "Item ${index + 1} (${item.productExternalId})"
            if (!Money.isPositive(item.price)) reasons += "$label: o preço deve ser maior que zero"
            if (!Money.hasAtMostTwoDecimals(item.price)) reasons += "$label: o preço tem mais de 2 casas decimais"
            if (item.quantity != null && item.quantity <= 0) reasons += "$label: a quantidade deve ser maior que zero"

            val product = products.findByPartnerIdAndExternalId(partner.id!!, item.productExternalId)
            when {
                product == null -> { reasons += "$label: produto não encontrado neste parceiro"; null }
                !product.active -> { reasons += "$label: produto inativo"; null }
                !product.isAvailable(now) -> { reasons += "$label: produto fora da validade"; null }
                else -> item to product
            }
        }

        // 2. Voucher (opcional).
        var voucher: VoucherEmitted? = null
        var remainingBefore = 0L
        if (!voucherCode.isNullOrBlank()) {
            val code = voucherCode.trim().uppercase()
            voucher = if (lockVoucher) vouchersEmitted.lockByCode(code) else vouchersEmitted.findByCode(code)
            when {
                voucher == null -> reasons += "Voucher $code não encontrado"
                voucher.clientId != client.id -> reasons += "Voucher $code pertence a outro cliente"
                else -> {
                    remainingBefore = voucherService.remainingPoints(voucher)
                    if (!voucher.isWithinValidity(now)) reasons += "Voucher $code venceu em ${formatDate(voucher.validUntil!!)}"
                    if (remainingBefore <= 0) reasons += "Voucher $code não tem mais crédito"
                    if (resolved.isNotEmpty() && resolved.none { (_, product) -> voucher.appliesTo(product) }) {
                        reasons += "Voucher $code não se aplica a nenhum produto deste pedido"
                    }
                }
            }
        }
        val usableVoucher = voucher?.takeIf { reasons.isEmpty() }

        // 3. Aplica o voucher nos itens elegíveis, na ordem enviada, e calcula os pontos de cada item.
        var available = if (usableVoucher != null) remainingBefore else 0L
        val results = resolved.map { (item, product) ->
            val rate = product.pointsPerCurrencyUnit
            val coverage = if (usableVoucher != null && usableVoucher.appliesTo(product)) {
                PointsCalculator.applyVoucher(item.price, rate, available)
            } else {
                PointsCalculator.Coverage(0, BigDecimal.ZERO.setScale(2), item.price)
            }
            available -= coverage.pointsUsed
            ItemResult(
                productExternalId = item.productExternalId,
                productId = product.id!!,
                productName = product.name,
                quantity = item.quantity,
                price = item.price,
                voucherPointsUsed = coverage.pointsUsed,
                coveredByVoucher = coverage.covered,
                paid = coverage.paid,
                pointsEarned = PointsCalculator.earned(coverage.paid, rate),
            )
        }

        val usage = usableVoucher?.let {
            VoucherUsage(it.code, remainingBefore, results.sumOf { r -> r.voucherPointsUsed }, available)
        }
        return OrderEvaluation(reasons, results, usage, usableVoucher)
    }

    private fun duplicateResponse(externalOrderId: String, clientId: Long, existing: List<Transaction>): OrderResponse {
        if (existing.first().clientId != clientId) {
            throw BusinessException("O pedido $externalOrderId já foi registrado para outro cliente", HttpStatus.CONFLICT)
        }
        val items = existing.map { t ->
            val product = products.findByIdOrNull(t.productId!!)!!
            ItemResult(product.externalId, product.id!!, product.name, t.quantity, t.price!!, t.voucherPointsUsed, null, null, t.totalPoints)
        }
        val voucher = existing.firstNotNullOfOrNull { it.voucherEmittedId }?.let { id ->
            val v = vouchersEmitted.findByIdOrNull(id)!!
            val used = items.sumOf { it.voucherPointsUsed }
            val remainingNow = voucherService.remainingPoints(v)
            VoucherUsage(v.code, remainingNow + used, used, remainingNow)
        }
        return OrderResponse(externalOrderId, true, items, voucher, items.sumOf { it.pointsEarned }, transactions.balanceOf(clientId))
    }

    private fun findPartner(partnerId: Long): Partner = partners.findByIdOrNull(partnerId) ?: throw notFound("Parceiro")

    /** Só clientes cadastrados no programa: e-mail desconhecido = nada é feito. */
    private fun findRegisteredClient(email: String): Client =
        clients.findByEmailIgnoreCase(email.trim())
            ?: throw BusinessException("Cliente $email não está cadastrado no programa", HttpStatus.NOT_FOUND)

    private fun formatDate(instant: Instant): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Sao_Paulo")).format(instant)
}
