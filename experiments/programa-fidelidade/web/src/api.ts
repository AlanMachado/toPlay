// Comunicação com o backend. Valores em dinheiro chegam e saem como texto ("149.90"):
// o front nunca faz conta com dinheiro nem com pontos, só exibe o que o backend calculou.

export type Client = { id: number; name: string; email: string; createdAt: string }
export type Balance = { clientId: number; points: number }

export type TransactionType = 'PURCHASE' | 'VOUCHER_EMISSION'
export type Transaction = {
  id: number
  type: TransactionType
  totalPoints: number
  createdAt: string
  productName: string | null
  partnerName: string | null
  quantity: number | null
  price: string | null
  voucherCode: string | null
  voucherPointsUsed: number
  externalOrderId: string | null
}

export type VoucherScope = {
  productId: number | null
  productName: string | null
  partnerId: number | null
  partnerName: string | null
}
export type AvailableVoucher = {
  id: number
  worthPoints: number
  validUntil: string | null
  scope: VoucherScope
  affordable: boolean
}
export type EmittedVoucherStatus = 'ACTIVE' | 'USED' | 'EXPIRED'
export type EmittedVoucher = {
  id: number
  code: string
  worthPoints: number
  pointsUsed: number
  remainingPoints: number
  validUntil: string | null
  scope: VoucherScope
  status: EmittedVoucherStatus
  createdAt: string
}

export type Partner = { id: number; name: string; active: boolean }
export type Product = {
  id: number
  name: string
  type: string | null
  partnerId: number
  pointsPerCurrencyUnit: string
  active: boolean
  validUntil: string | null
  externalId: string
}

export type OrderItem = { productExternalId: string; quantity?: number; price: string }
export type ItemResult = {
  productExternalId: string
  productId: number
  productName: string
  quantity: number | null
  price: string
  voucherPointsUsed: number
  coveredByVoucher: string | null
  paid: string | null
  pointsEarned: number
}
export type VoucherUsage = {
  code: string
  remainingBefore: number
  pointsUsed: number
  remainingAfter: number
  fullyUsed: boolean
}
export type OrderResponse = {
  externalOrderId: string
  duplicate: boolean
  items: ItemResult[]
  voucher: VoucherUsage | null
  totalPointsEarned: number
  clientBalance: number
}
export type VoucherCheckResponse = {
  valid: boolean
  reasons: string[]
  voucher: VoucherUsage | null
  items: ItemResult[]
}

export class ApiError extends Error {
  readonly status: number
  readonly reasons: string[]
  constructor(message: string, status: number, reasons: string[]) {
    super(message)
    this.status = status
    this.reasons = reasons
  }
}

async function call<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message = body?.error ?? `Erro ${response.status}`
    throw new ApiError(message, response.status, body?.reasons ?? [message])
  }
  return response.json() as Promise<T>
}

const post = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })

export const api = {
  findClient: (email: string) => call<Client>(`/clients?email=${encodeURIComponent(email)}`),
  createClient: (name: string, email: string) => call<Client>('/clients', post({ name, email })),
  balance: (clientId: number) => call<Balance>(`/clients/${clientId}/balance`),
  transactions: (clientId: number) => call<Transaction[]>(`/clients/${clientId}/transactions`),

  availableVouchers: (clientId: number) => call<AvailableVoucher[]>(`/clients/${clientId}/available-vouchers`),
  emitVoucher: (clientId: number, voucherId: number) =>
    call<EmittedVoucher>(`/clients/${clientId}/vouchers`, post({ voucherId })),
  myVouchers: (clientId: number) => call<EmittedVoucher[]>(`/clients/${clientId}/vouchers`),

  partners: () => call<Partner[]>('/partners'),
  products: (partnerId: number) => call<Product[]>(`/partners/${partnerId}/products`),
  validateVoucher: (partnerId: number, clientEmail: string, voucherCode: string, items: OrderItem[]) =>
    call<VoucherCheckResponse>(`/partners/${partnerId}/vouchers/validate`, post({ clientEmail, voucherCode, items })),
  registerOrder: (
    partnerId: number,
    order: { externalOrderId: string; clientEmail: string; voucherCode?: string; items: OrderItem[] },
  ) => call<OrderResponse>(`/partners/${partnerId}/orders`, post(order)),
}
