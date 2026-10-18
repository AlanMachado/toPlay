package br.loyalty.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Regras de dinheiro do sistema, centralizadas num lugar só.
 *
 * - Nunca use Double/Float para valores. Crie BigDecimal a partir de String: BigDecimal("0.10").
 * - Compare com compareTo (ou os helpers abaixo), nunca com ==, porque 2.0 != 2.00 para o BigDecimal.
 * - Toda divisão deve informar escala e arredondamento explicitamente.
 */
object Money {
    const val SCALE = 2

    fun isPositive(value: BigDecimal): Boolean = value.signum() > 0

    fun hasAtMostTwoDecimals(value: BigDecimal): Boolean = value.stripTrailingZeros().scale() <= SCALE

    /** Exibição em mensagens: "1234.5" -> "R$ 1234,50". */
    fun format(value: BigDecimal): String = "R$ " + value.setScale(SCALE, RoundingMode.HALF_EVEN).toPlainString().replace('.', ',')
}

/** Valores trafegam no JSON como texto ("149.90") para o JavaScript não corromper as casas decimais. */
@Configuration
class MoneyJsonConfig {
    @Bean
    fun bigDecimalAsString() = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.serializerByType(BigDecimal::class.java, object : JsonSerializer<BigDecimal>() {
            override fun serialize(value: BigDecimal, gen: JsonGenerator, provider: SerializerProvider) {
                gen.writeString(value.toPlainString())
            }
        })
    }
}
