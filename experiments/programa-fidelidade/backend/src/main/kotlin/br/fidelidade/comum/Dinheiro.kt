package br.fidelidade.comum

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
 * - Toda divisão ou valor final deve passar por um arredondamento explícito.
 */
object Dinheiro {
    /** Arredondamento bancário: 0,125 -> 0,12 e 0,135 -> 0,14. Evita viés acumulado. */
    val ARREDONDAMENTO: RoundingMode = RoundingMode.HALF_EVEN

    /** Valor final em reais, com 2 casas. */
    fun emReais(valor: BigDecimal): BigDecimal = valor.setScale(2, ARREDONDAMENTO)
}

fun BigDecimal.ehPositivo(): Boolean = this.signum() > 0

/** Valores trafegam no JSON como texto ("149.90") para o JavaScript não corromper as casas decimais. */
@Configuration
class JsonDinheiroConfig {
    @Bean
    fun bigDecimalComoTexto() = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.serializerByType(BigDecimal::class.java, object : JsonSerializer<BigDecimal>() {
            override fun serialize(valor: BigDecimal, gen: JsonGenerator, provider: SerializerProvider) {
                gen.writeString(valor.toPlainString())
            }
        })
    }
}
