package br.loyalty.order

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Contas de pontos. A taxa do produto (pontos por unidade de moeda) vale nos dois sentidos:
 * para gerar pontos e para saber quanto um voucher precisa consumir para cobrir um valor.
 *
 * Todos os arredondamentos favorecem o programa: o cliente nunca ganha pontos ou cobertura
 * sem ter pago por eles.
 */
object PointsCalculator {
    private val ZERO_MONEY: BigDecimal = BigDecimal.ZERO.setScale(2)

    /** Pontos gerados pelo valor efetivamente pago, arredondado para baixo. Ex.: R$ 30,00 × 100 = 3.000. */
    fun earned(paid: BigDecimal, rate: BigDecimal): Long = (paid * rate).setScale(0, RoundingMode.DOWN).longValueExact()

    /** Quanto um valor custa em pontos, arredondado para cima. */
    fun cost(value: BigDecimal, rate: BigDecimal): Long = (value * rate).setScale(0, RoundingMode.CEILING).longValueExact()

    data class Coverage(
        /** Pontos consumidos do voucher. */
        val pointsUsed: Long,
        /** Parte do preço coberta pelo voucher. */
        val covered: BigDecimal,
        /** Parte do preço paga pelo cliente (é a que gera pontos). */
        val paid: BigDecimal,
    )

    /**
     * Aplica o crédito disponível de um voucher sobre o preço de um item.
     *
     * - Crédito suficiente: cobre o item todo e consome exatamente o custo do item em pontos.
     * - Crédito insuficiente: cobre o que der (arredondado para baixo em centavos) e consome só
     *   os pontos necessários para essa cobertura; o resto continua no voucher.
     */
    fun applyVoucher(price: BigDecimal, rate: BigDecimal, availablePoints: Long): Coverage {
        if (availablePoints <= 0) return Coverage(0, ZERO_MONEY, price)

        val fullCost = cost(price, rate)
        if (availablePoints >= fullCost) return Coverage(fullCost, price, ZERO_MONEY)

        val covered = BigDecimal.valueOf(availablePoints).divide(rate, 2, RoundingMode.DOWN)
        return Coverage(cost(covered, rate), covered, price - covered)
    }
}
