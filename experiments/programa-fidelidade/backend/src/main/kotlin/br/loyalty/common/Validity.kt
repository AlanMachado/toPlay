package br.loyalty.common

import java.time.Instant

/** Regra única de validade: sem data = sempre válido; com data = válido até ela (exclusive). */
fun isWithinValidity(validUntil: Instant?, now: Instant): Boolean = validUntil == null || now.isBefore(validUntil)
