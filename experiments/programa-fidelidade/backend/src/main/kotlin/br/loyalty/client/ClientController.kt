package br.loyalty.client

import br.loyalty.common.BusinessException
import br.loyalty.common.notFound
import br.loyalty.ledger.TransactionRepository
import br.loyalty.ledger.TransactionType
import br.loyalty.partner.PartnerRepository
import br.loyalty.product.ProductRepository
import br.loyalty.voucher.VoucherEmittedRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

data class NewClientRequest(
    @field:NotBlank val name: String?,
    @field:NotBlank @field:Email val email: String?,
)

data class ClientDto(val id: Long, val name: String, val email: String, val createdAt: Instant)

data class BalanceDto(val clientId: Long, val points: Long)

data class TransactionDto(
    val id: Long,
    val type: TransactionType,
    val totalPoints: Long,
    val createdAt: Instant,
    val productName: String?,
    val partnerName: String?,
    val quantity: Int?,
    val price: BigDecimal?,
    val voucherCode: String?,
    val voucherPointsUsed: Long,
    val externalOrderId: String?,
)

private fun Client.toDto() = ClientDto(id!!, name, email, createdAt)

@RestController
@RequestMapping("/api/clients")
class ClientController(
    private val clients: ClientRepository,
    private val transactions: TransactionRepository,
    private val products: ProductRepository,
    private val partners: PartnerRepository,
    private val vouchersEmitted: VoucherEmittedRepository,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: NewClientRequest): ClientDto {
        val email = request.email!!.trim().lowercase()
        if (clients.findByEmailIgnoreCase(email) != null) {
            throw BusinessException("Já existe um cliente com este e-mail", HttpStatus.CONFLICT)
        }
        return clients.save(Client(name = request.name!!.trim(), email = email)).toDto()
    }

    /** "Login" provisório por e-mail. Ainda não há senha nem autenticação de verdade. */
    @GetMapping
    fun findByEmail(@RequestParam email: String): ClientDto =
        (clients.findByEmailIgnoreCase(email.trim()) ?: throw notFound("Cliente")).toDto()

    @GetMapping("/{id}/balance")
    fun balance(@PathVariable id: Long): BalanceDto {
        ensureExists(id)
        return BalanceDto(id, transactions.balanceOf(id))
    }

    /** Extrato: as últimas 50 transações, com nomes de produto, parceiro e voucher. */
    @GetMapping("/{id}/transactions")
    fun transactions(@PathVariable id: Long): List<TransactionDto> {
        ensureExists(id)
        val page = transactions.findByClientIdOrderByCreatedAtDescIdDesc(id, PageRequest.of(0, 50))
        val productsById = products.findAllById(page.mapNotNull { it.productId }.toSet()).associateBy { it.id!! }
        val partnersById = partners.findAllById(page.mapNotNull { it.partnerId }.toSet()).associateBy { it.id!! }
        val vouchersById = vouchersEmitted.findAllById(page.mapNotNull { it.voucherEmittedId }.toSet()).associateBy { it.id!! }
        return page.map { t ->
            TransactionDto(
                id = t.id!!,
                type = t.type,
                totalPoints = t.totalPoints,
                createdAt = t.createdAt,
                productName = t.productId?.let { productsById[it]?.name },
                partnerName = t.partnerId?.let { partnersById[it]?.name },
                quantity = t.quantity,
                price = t.price,
                voucherCode = t.voucherEmittedId?.let { vouchersById[it]?.code },
                voucherPointsUsed = t.voucherPointsUsed,
                externalOrderId = t.externalOrderId,
            )
        }
    }

    private fun ensureExists(id: Long) {
        if (!clients.existsById(id)) throw notFound("Cliente")
    }
}
