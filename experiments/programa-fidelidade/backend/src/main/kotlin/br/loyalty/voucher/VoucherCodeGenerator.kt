package br.loyalty.voucher

import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * Gera códigos no formato "K7QX-M2PA". O alfabeto não tem caracteres fáceis de confundir
 * ao digitar ou ler em voz alta (0/O, 1/I/L).
 */
@Component
class VoucherCodeGenerator(private val vouchers: VoucherEmittedRepository) {
    private val random = SecureRandom()

    fun newUniqueCode(): String {
        repeat(10) {
            val code = randomCode()
            if (!vouchers.existsByCode(code)) return code
        }
        error("Não foi possível gerar um código de voucher único")
    }

    private fun randomCode(): String {
        val chars = (1..8).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
        return chars.take(4) + "-" + chars.drop(4)
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
