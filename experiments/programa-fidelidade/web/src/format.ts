// Formatação para exibição. O dinheiro é formatado a partir do texto, sem passar por
// Number, para nunca introduzir erro de ponto flutuante.

export function formatPoints(points: number): string {
  return points.toLocaleString('pt-BR')
}

/** "1234.5" -> "R$ 1.234,50" */
export function formatMoney(value: string): string {
  const negative = value.startsWith('-')
  const [integer, cents = ''] = value.replace('-', '').split('.')
  const thousands = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `${negative ? '-' : ''}R$ ${thousands},${cents.padEnd(2, '0').slice(0, 2)}`
}

/** Aceita "149,90" ou "149.90" digitado pelo usuário e devolve "149.90", ou null se inválido. */
export function parseMoneyInput(text: string): string | null {
  const t = text.trim()
  // Com vírgula, os pontos são separadores de milhar ("1.234,56"); sem vírgula, o ponto é decimal.
  const clean = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t
  return /^\d+(\.\d{1,2})?$/.test(clean) ? clean : null
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

/** A quem um voucher se aplica: o produto tem precedência sobre o parceiro. */
export function describeScope(scope: { productName: string | null; partnerName: string | null }): string {
  if (scope.productName) return `Só para ${scope.productName}${scope.partnerName ? ` (${scope.partnerName})` : ''}`
  return `Qualquer produto de ${scope.partnerName ?? 'parceiro'}`
}

export function describeValidity(validUntil: string | null): string {
  return validUntil ? `Válido até ${formatDate(validUntil)}` : 'Sem data de validade'
}
