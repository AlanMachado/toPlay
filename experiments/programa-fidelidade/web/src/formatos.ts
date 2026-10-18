// Formatação para exibição. O dinheiro é formatado a partir do texto, sem passar por
// Number, para nunca introduzir erro de ponto flutuante.

export function formatarPontos(pontos: number): string {
  return pontos.toLocaleString('pt-BR')
}

/** "1234.5" -> "R$ 1.234,50" */
export function formatarReais(valor: string): string {
  const negativo = valor.startsWith('-')
  const [inteiro, centavos = ''] = valor.replace('-', '').split('.')
  const milhares = inteiro.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `${negativo ? '-' : ''}R$ ${milhares},${centavos.padEnd(2, '0').slice(0, 2)}`
}

/** Aceita "149,90" ou "149.90" digitado pelo usuário e devolve "149.90", ou null se inválido. */
export function normalizarValorDigitado(texto: string): string | null {
  const t = texto.trim()
  // Com vírgula, os pontos são separadores de milhar ("1.234,56"); sem vírgula, o ponto é decimal.
  const limpo = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t
  const valido = /^\d+(\.\d{1,2})?$/.test(limpo)
  return valido ? limpo : null
}

export function formatarData(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}
