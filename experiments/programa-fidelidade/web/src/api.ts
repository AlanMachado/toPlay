// Comunicação com o backend. Valores em dinheiro chegam e saem como texto ("149.90"):
// o front nunca faz conta com dinheiro, só exibe o que o backend calculou.

export type Cliente = { id: number; nome: string; email: string }
export type Saldo = { clienteId: number; pontos: number }
export type TipoLancamento = 'CREDITO' | 'RESGATE' | 'ESTORNO' | 'VENCIMENTO'
export type Lancamento = {
  id: number
  tipo: TipoLancamento
  pontos: number
  valor: string | null
  descricao: string
  criadoEm: string
}
export type ItemCatalogo = {
  id: number
  nome: string
  descricao: string | null
  parceiroId: number
  parceiroNome: string
  preco: string
  /** Calculado pelo backend: preço do item ÷ preço do ponto do parceiro (ou valor promocional). */
  custoPontos: number
  custoPontosSemPromocao: number
  emPromocao: boolean
}
export type Parceiro = { id: number; nome: string; precoPorPonto: string; pontosPorReal: string }
export type ResultadoCompra = {
  pontosGanhos: number
  valorCompra: string
  valorDevidoPeloParceiro: string
}
export type ResultadoResgate = { resgateId: number; pontosUsados: number; saldoRestante: number }

export class ErroApi extends Error {
  readonly status: number
  constructor(mensagem: string, status: number) {
    super(mensagem)
    this.status = status
  }
}

async function chamar<T>(caminho: string, opcoes?: RequestInit): Promise<T> {
  const resposta = await fetch(`/api${caminho}`, {
    headers: { 'Content-Type': 'application/json' },
    ...opcoes,
  })
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null)
    throw new ErroApi(corpo?.erro ?? `Erro ${resposta.status}`, resposta.status)
  }
  return resposta.json() as Promise<T>
}

const enviar = (corpo: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(corpo) })

export const api = {
  buscarCliente: (email: string) => chamar<Cliente>(`/clientes?email=${encodeURIComponent(email)}`),
  cadastrarCliente: (nome: string, email: string) => chamar<Cliente>('/clientes', enviar({ nome, email })),
  saldo: (clienteId: number) => chamar<Saldo>(`/clientes/${clienteId}/saldo`),
  extrato: (clienteId: number) => chamar<Lancamento[]>(`/clientes/${clienteId}/extrato`),
  catalogo: () => chamar<ItemCatalogo[]>('/catalogo'),
  parceiros: () => chamar<Parceiro[]>('/parceiros'),
  registrarCompra: (parceiroId: number, clienteId: number, valorCompra: string) =>
    chamar<ResultadoCompra>(`/parceiros/${parceiroId}/compras`, enviar({ clienteId, valorCompra })),
  resgatar: (clienteId: number, itemId: number) =>
    chamar<ResultadoResgate>('/resgates', enviar({ clienteId, itemId })),
}
