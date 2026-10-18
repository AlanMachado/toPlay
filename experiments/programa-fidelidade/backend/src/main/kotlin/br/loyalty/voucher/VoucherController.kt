package br.loyalty.voucher

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class EmitVoucherRequest(@field:NotNull val voucherId: Long?)

/** Vouchers do ponto de vista do cliente (app). */
@RestController
@RequestMapping("/api/clients/{clientId}")
class VoucherController(private val vouchers: VoucherService) {

    /** Modelos de voucher ativos e válidos, indicando quais o cliente consegue pagar. */
    @GetMapping("/available-vouchers")
    fun available(@PathVariable clientId: Long): List<AvailableVoucherDto> = vouchers.availableFor(clientId)

    /** Emite um voucher: debita os pontos e devolve o código. */
    @PostMapping("/vouchers")
    @ResponseStatus(HttpStatus.CREATED)
    fun emit(@PathVariable clientId: Long, @Valid @RequestBody request: EmitVoucherRequest): EmittedVoucherDto =
        vouchers.emit(clientId, request.voucherId!!)

    /** Vouchers já emitidos pelo cliente, com o crédito restante. */
    @GetMapping("/vouchers")
    fun mine(@PathVariable clientId: Long): List<EmittedVoucherDto> = vouchers.emittedFor(clientId)
}
