import {
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonCol,
  IonContent,
  IonGrid,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonList,
  IonListHeader,
  IonLabel,
  IonPage,
  IonRow,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { addOutline, trashOutline } from 'ionicons/icons'
import { useEffect, useState } from 'react'
import { api, ApiError, type ItemResult, type OrderItem, type Partner, type Product, type VoucherUsage } from '../api'
import { formatMoney, formatPoints, parseMoneyInput } from '../format'
import { useLoggedClient } from '../session'

type ItemRow = { key: number; productExternalId: string; quantity: string; price: string }

type Result =
  | { kind: 'validation'; valid: boolean; reasons: string[]; voucher: VoucherUsage | null; items: ItemResult[] }
  | { kind: 'order'; duplicate: boolean; voucher: VoucherUsage | null; items: ItemResult[]; total: number; balance: number }
  | { kind: 'error'; reasons: string[] }

let nextKey = 1
const newRow = (): ItemRow => ({ key: nextKey++, productExternalId: '', quantity: '', price: '' })
const newOrderId = () => `PED-${Date.now().toString().slice(-8)}`

/**
 * Faz o papel do sistema do parceiro: monta um pedido e chama a API de validação de voucher
 * e de registro de pedido. Num produto real, isto roda no sistema da loja, não no app do cliente.
 */
export default function PartnerSimulator() {
  const client = useLoggedClient()
  const [partners, setPartners] = useState<Partner[]>([])
  const [partnerId, setPartnerId] = useState<number | null>(null)
  const [products, setProducts] = useState<Product[]>([])
  const [clientEmail, setClientEmail] = useState(client.email)
  const [orderId, setOrderId] = useState(newOrderId)
  const [voucherCode, setVoucherCode] = useState('')
  const [rows, setRows] = useState<ItemRow[]>([newRow()])
  const [result, setResult] = useState<Result | null>(null)

  async function selectPartner(id: number) {
    setPartnerId(id)
    setRows([newRow()])
    setResult(null)
    setProducts(await api.products(id))
  }

  // Carrega os parceiros uma vez só. (Com useIonViewWillEnter, voltar para a aba
  // reselecionaria o parceiro e apagaria os itens já preenchidos.)
  useEffect(() => {
    api.partners().then((list) => {
      setPartners(list)
      if (list[0]) selectPartner(list[0].id)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const updateRow = (key: number, change: Partial<ItemRow>) =>
    setRows((current) => current.map((r) => (r.key === key ? { ...r, ...change } : r)))

  // Monta os itens do pedido; null se algum estiver incompleto ou inválido.
  const items: OrderItem[] | null = (() => {
    const built: OrderItem[] = []
    for (const r of rows) {
      const price = parseMoneyInput(r.price)
      const quantity = r.quantity.trim() === '' ? undefined : Number(r.quantity)
      if (!r.productExternalId || !price) return null
      if (quantity !== undefined && (!Number.isInteger(quantity) || quantity <= 0)) return null
      built.push({ productExternalId: r.productExternalId, price, ...(quantity ? { quantity } : {}) })
    }
    return built
  })()

  async function run(action: () => Promise<Result>) {
    try {
      setResult(await action())
    } catch (e) {
      setResult({ kind: 'error', reasons: e instanceof ApiError ? e.reasons : [String(e)] })
    }
  }

  const validate = () =>
    run(async () => {
      const r = await api.validateVoucher(partnerId!, clientEmail, voucherCode, items!)
      return { kind: 'validation', ...r }
    })

  const register = () =>
    run(async () => {
      const r = await api.registerOrder(partnerId!, {
        externalOrderId: orderId,
        clientEmail,
        items: items!,
        ...(voucherCode.trim() ? { voucherCode } : {}),
      })
      if (!r.duplicate) setOrderId(newOrderId())
      return { kind: 'order', duplicate: r.duplicate, voucher: r.voucher, items: r.items, total: r.totalPointsEarned, balance: r.clientBalance }
    })

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Simular parceiro</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonList>
          <IonItem>
            <IonSelect
              label="Parceiro"
              labelPlacement="stacked"
              value={partnerId}
              onIonChange={(e) => selectPartner(Number(e.detail.value))}
            >
              {partners.map((p) => (
                <IonSelectOption key={p.id} value={p.id}>
                  {p.name}
                </IonSelectOption>
              ))}
            </IonSelect>
          </IonItem>
          <IonItem>
            <IonInput
              label="E-mail do cliente"
              labelPlacement="stacked"
              type="email"
              value={clientEmail}
              onIonInput={(e) => setClientEmail(String(e.detail.value ?? ''))}
            />
          </IonItem>
          <IonItem>
            <IonInput
              label="Id do pedido no parceiro"
              labelPlacement="stacked"
              value={orderId}
              onIonInput={(e) => setOrderId(String(e.detail.value ?? ''))}
            />
          </IonItem>
          <IonItem>
            <IonInput
              label="Código do voucher (opcional)"
              labelPlacement="stacked"
              placeholder="ABCD-1234"
              value={voucherCode}
              onIonInput={(e) => setVoucherCode(String(e.detail.value ?? '').toUpperCase())}
            />
          </IonItem>

          <IonListHeader>
            <IonLabel>Itens do pedido</IonLabel>
          </IonListHeader>
          {rows.map((row) => (
            <IonItem key={row.key}>
              <IonGrid className="ion-no-padding">
                <IonRow>
                  <IonCol size="12">
                    <IonSelect
                      label="Produto"
                      labelPlacement="stacked"
                      placeholder="Escolha"
                      value={row.productExternalId}
                      onIonChange={(e) => updateRow(row.key, { productExternalId: String(e.detail.value) })}
                    >
                      {products.map((p) => (
                        <IonSelectOption key={p.id} value={p.externalId} disabled={!p.active}>
                          {p.name} · {p.pointsPerCurrencyUnit.replace(/\.?0+$/, '')} pts/R$
                        </IonSelectOption>
                      ))}
                    </IonSelect>
                  </IonCol>
                  <IonCol size="4">
                    <IonInput
                      label="Qtd."
                      labelPlacement="stacked"
                      inputmode="numeric"
                      placeholder="opcional"
                      value={row.quantity}
                      onIonInput={(e) => updateRow(row.key, { quantity: String(e.detail.value ?? '') })}
                    />
                  </IonCol>
                  <IonCol size="6">
                    <IonInput
                      label="Preço total (R$)"
                      labelPlacement="stacked"
                      inputmode="decimal"
                      placeholder="30,00"
                      value={row.price}
                      onIonInput={(e) => updateRow(row.key, { price: String(e.detail.value ?? '') })}
                    />
                  </IonCol>
                  <IonCol size="2" className="ion-align-self-end">
                    <IonButton
                      fill="clear"
                      color="danger"
                      disabled={rows.length === 1}
                      aria-label="Remover item"
                      onClick={() => setRows((current) => current.filter((r) => r.key !== row.key))}
                    >
                      <IonIcon icon={trashOutline} />
                    </IonButton>
                  </IonCol>
                </IonRow>
              </IonGrid>
            </IonItem>
          ))}
        </IonList>

        <div className="ion-padding-horizontal">
          <IonButton fill="outline" size="small" onClick={() => setRows((current) => [...current, newRow()])}>
            <IonIcon slot="start" icon={addOutline} />
            Adicionar item
          </IonButton>
          {!items && (
            <IonText color="medium">
              <p>Escolha o produto e informe o preço de cada item (até 2 casas decimais).</p>
            </IonText>
          )}
          <IonButton expand="block" fill="outline" disabled={!items || !voucherCode.trim()} onClick={validate}>
            Validar voucher
          </IonButton>
          <IonButton expand="block" disabled={!items || !orderId.trim() || !clientEmail.trim()} onClick={register}>
            Registrar pedido
          </IonButton>
        </div>

        {result && <ResultCard result={result} />}
      </IonContent>
    </IonPage>
  )
}

function ResultCard({ result }: { result: Result }) {
  if (result.kind === 'error' || (result.kind === 'validation' && !result.valid)) {
    return (
      <IonCard color="danger">
        <IonCardHeader>
          <IonCardTitle>{result.kind === 'error' ? 'Pedido recusado' : 'Voucher inválido'}</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <ul>
            {result.reasons.map((r) => (
              <li key={r}>{r}</li>
            ))}
          </ul>
        </IonCardContent>
      </IonCard>
    )
  }

  const title =
    result.kind === 'validation'
      ? 'Voucher válido'
      : result.duplicate
        ? 'Pedido já registrado (nada foi creditado de novo)'
        : `Pedido registrado: +${formatPoints(result.total)} pontos`

  return (
    <IonCard color={result.kind === 'order' && result.duplicate ? 'warning' : 'success'}>
      <IonCardHeader>
        <IonCardTitle>{title}</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <table className="result-table">
          <thead>
            <tr>
              <th>Produto</th>
              <th>Preço</th>
              <th>Voucher</th>
              <th>Pontos</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((i, index) => (
              <tr key={index}>
                <td>{i.productName}</td>
                <td>{formatMoney(i.price)}</td>
                <td>
                  {i.voucherPointsUsed > 0
                    ? `−${formatPoints(i.voucherPointsUsed)} pts${i.coveredByVoucher ? ` (${formatMoney(i.coveredByVoucher)})` : ''}`
                    : '—'}
                </td>
                <td>+{formatPoints(i.pointsEarned)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {result.voucher && (
          <p>
            Voucher {result.voucher.code}: {formatPoints(result.voucher.pointsUsed)} pts usados,{' '}
            {result.voucher.fullyUsed
              ? 'crédito esgotado.'
              : `restam ${formatPoints(result.voucher.remainingAfter)} pts.`}
          </p>
        )}
        {result.kind === 'order' && <p>Saldo do cliente: {formatPoints(result.balance)} pontos</p>}
      </IonCardContent>
    </IonCard>
  )
}
