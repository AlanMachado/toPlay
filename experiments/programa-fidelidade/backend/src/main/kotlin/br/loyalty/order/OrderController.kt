package br.loyalty.order

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class OrderItemRequest(
    @field:NotBlank val productExternalId: String?,
    /** Opcional, só para registro: os pontos são calculados sobre o preço total. */
    val quantity: Int?,
    /** Preço total do item, como texto no JSON: "30.00". */
    @field:NotNull val price: BigDecimal?,
) {
    fun toInput() = OrderItemInput(productExternalId!!.trim(), quantity, price!!)
}

data class OrderRequest(
    @field:NotBlank val externalOrderId: String?,
    @field:NotBlank @field:Email val clientEmail: String?,
    val voucherCode: String?,
    @field:NotEmpty @field:Valid val items: List<OrderItemRequest>?,
)

data class VoucherCheckRequest(
    @field:NotBlank @field:Email val clientEmail: String?,
    @field:NotBlank val voucherCode: String?,
    @field:NotEmpty @field:Valid val items: List<OrderItemRequest>?,
)

/** Endpoints chamados pelo sistema do parceiro. */
@RestController
@RequestMapping("/api/partners/{partnerId}")
class OrderController(private val orders: OrderService) {

    /** Valida um voucher para um pedido antes de enviá-lo. Não grava nada. */
    @PostMapping("/vouchers/validate")
    fun validateVoucher(@PathVariable partnerId: Long, @Valid @RequestBody request: VoucherCheckRequest): VoucherCheckResponse =
        orders.checkVoucher(partnerId, request.clientEmail!!, request.voucherCode!!, request.items!!.map { it.toInput() })

    /** Registra um pedido: credita os pontos e consome o voucher, se houver. */
    @PostMapping("/orders")
    fun registerOrder(@PathVariable partnerId: Long, @Valid @RequestBody request: OrderRequest): ResponseEntity<OrderResponse> {
        val response = orders.register(
            partnerId = partnerId,
            externalOrderId = request.externalOrderId!!.trim(),
            clientEmail = request.clientEmail!!,
            voucherCode = request.voucherCode,
            items = request.items!!.map { it.toInput() },
        )
        // 201 quando gravou; 200 quando era um reenvio de pedido já registrado.
        return ResponseEntity.status(if (response.duplicate) HttpStatus.OK else HttpStatus.CREATED).body(response)
    }
}
