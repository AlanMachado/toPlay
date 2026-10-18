package br.loyalty.partner

import br.loyalty.common.BusinessException
import br.loyalty.common.notFound
import br.loyalty.product.Product
import br.loyalty.product.ProductRepository
import br.loyalty.voucher.Voucher
import br.loyalty.voucher.VoucherRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

data class PartnerDto(val id: Long, val name: String, val active: Boolean)

data class NewPartnerRequest(@field:NotBlank val name: String?, val active: Boolean = true)

data class ProductDto(
    val id: Long,
    val name: String,
    val type: String?,
    val partnerId: Long,
    val pointsPerCurrencyUnit: BigDecimal,
    val active: Boolean,
    val validUntil: Instant?,
    val externalId: String,
)

data class NewProductRequest(
    @field:NotBlank val name: String?,
    val type: String?,
    @field:NotNull @field:Positive val pointsPerCurrencyUnit: BigDecimal?,
    val active: Boolean = true,
    val validUntil: Instant?,
    @field:NotBlank val externalId: String?,
)

data class VoucherTemplateDto(
    val id: Long,
    val productId: Long?,
    val partnerId: Long?,
    val worthPoints: Long,
    val active: Boolean,
    val validUntil: Instant?,
)

data class NewVoucherTemplateRequest(
    val productId: Long?,
    val partnerId: Long?,
    @field:NotNull @field:Positive val worthPoints: Long?,
    val active: Boolean = true,
    val validUntil: Instant?,
)

private fun Partner.toDto() = PartnerDto(id!!, name, active)

private fun Product.toDto() = ProductDto(id!!, name, type, partnerId, pointsPerCurrencyUnit, active, validUntil, externalId)

private fun Voucher.toDto() = VoucherTemplateDto(id!!, productId, partnerId, worthPoints, active, validUntil)

/** Cadastros básicos (sem autenticação por enquanto). */
@RestController
class AdminController(
    private val partners: PartnerRepository,
    private val products: ProductRepository,
    private val vouchers: VoucherRepository,
) {
    @GetMapping("/api/partners")
    fun listPartners(): List<PartnerDto> = partners.findAll().sortedBy { it.name }.map { it.toDto() }

    @PostMapping("/api/partners")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPartner(@Valid @RequestBody request: NewPartnerRequest): PartnerDto =
        partners.save(Partner(name = request.name!!.trim(), active = request.active)).toDto()

    @GetMapping("/api/partners/{partnerId}/products")
    fun listProducts(@PathVariable partnerId: Long): List<ProductDto> {
        if (!partners.existsById(partnerId)) throw notFound("Parceiro")
        return products.findByPartnerIdOrderByName(partnerId).map { it.toDto() }
    }

    @PostMapping("/api/partners/{partnerId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@PathVariable partnerId: Long, @Valid @RequestBody request: NewProductRequest): ProductDto {
        if (!partners.existsById(partnerId)) throw notFound("Parceiro")
        val externalId = request.externalId!!.trim()
        if (products.findByPartnerIdAndExternalId(partnerId, externalId) != null) {
            throw BusinessException("Este parceiro já tem um produto com externalId $externalId", HttpStatus.CONFLICT)
        }
        return products.save(
            Product(
                name = request.name!!.trim(),
                type = request.type?.trim(),
                partnerId = partnerId,
                pointsPerCurrencyUnit = request.pointsPerCurrencyUnit!!,
                active = request.active,
                validUntil = request.validUntil,
                externalId = externalId,
            ),
        ).toDto()
    }

    @GetMapping("/api/voucher-templates")
    fun listVoucherTemplates(): List<VoucherTemplateDto> = vouchers.findAll().sortedBy { it.id }.map { it.toDto() }

    @PostMapping("/api/voucher-templates")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVoucherTemplate(@Valid @RequestBody request: NewVoucherTemplateRequest): VoucherTemplateDto {
        if (request.productId == null && request.partnerId == null) {
            throw BusinessException("Informe productId ou partnerId (productId tem precedência)")
        }
        if (request.productId != null && !products.existsById(request.productId)) throw notFound("Produto")
        if (request.partnerId != null && !partners.existsById(request.partnerId)) throw notFound("Parceiro")
        return vouchers.save(
            Voucher(
                productId = request.productId,
                partnerId = request.partnerId,
                worthPoints = request.worthPoints!!,
                active = request.active,
                validUntil = request.validUntil,
            ),
        ).toDto()
    }
}
