import {
  IonButton,
  IonButtons,
  IonCard,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonNote,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonText,
  IonTitle,
  IonToolbar,
  useIonViewWillEnter,
} from '@ionic/react'
import { useState } from 'react'
import { api, type Transaction } from '../api'
import { formatDate, formatMoney, formatPoints } from '../format'
import { useLoggedClient, useSession } from '../session'

function describe(t: Transaction): { title: string; details: string[] } {
  if (t.type === 'VOUCHER_EMISSION') {
    return { title: `Emissão do voucher ${t.voucherCode}`, details: [] }
  }
  const details = [`${t.partnerName} · ${formatMoney(t.price ?? '0')}${t.quantity ? ` · ${t.quantity} un.` : ''}`]
  if (t.voucherPointsUsed > 0) details.push(`Voucher ${t.voucherCode}: ${formatPoints(t.voucherPointsUsed)} pts usados`)
  return { title: t.productName ?? 'Compra', details }
}

export default function Balance() {
  const client = useLoggedClient()
  const { signOut } = useSession()
  const [points, setPoints] = useState<number | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [error, setError] = useState<string | null>(null)

  async function load() {
    try {
      const [balance, list] = await Promise.all([api.balance(client.id), api.transactions(client.id)])
      setPoints(balance.points)
      setTransactions(list)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Não foi possível carregar o saldo')
    }
  }

  // Recarrega sempre que a aba aparece (ex.: depois de emitir um voucher ou registrar um pedido).
  useIonViewWillEnter(() => {
    load()
  })

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Olá, {client.name.split(' ')[0]}</IonTitle>
          <IonButtons slot="end">
            <IonButton onClick={signOut}>Sair</IonButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonRefresher slot="fixed" onIonRefresh={(e) => load().finally(() => e.detail.complete())}>
          <IonRefresherContent />
        </IonRefresher>

        <IonCard className="balance-card">
          <IonNote>Seu saldo</IonNote>
          <div className="balance-value">{points === null ? '—' : formatPoints(points)}</div>
          <IonNote>pontos</IonNote>
        </IonCard>

        {error && (
          <IonText color="danger">
            <p className="ion-padding-horizontal">{error}</p>
          </IonText>
        )}

        <IonList>
          <IonListHeader>
            <IonLabel>Extrato</IonLabel>
          </IonListHeader>
          {transactions.length === 0 && (
            <IonItem lines="none">
              <IonLabel color="medium">
                Nenhuma movimentação ainda. Use "Simular parceiro" para registrar uma compra.
              </IonLabel>
            </IonItem>
          )}
          {transactions.map((t) => {
            const { title, details } = describe(t)
            return (
              <IonItem key={t.id}>
                <IonLabel>
                  <h3>{title}</h3>
                  {details.map((d) => (
                    <p key={d}>{d}</p>
                  ))}
                  <p>{formatDate(t.createdAt)}</p>
                </IonLabel>
                <span slot="end" className={t.totalPoints < 0 ? 'points-negative' : 'points-positive'}>
                  {t.totalPoints > 0 ? '+' : ''}
                  {formatPoints(t.totalPoints)}
                </span>
              </IonItem>
            )
          })}
        </IonList>
      </IonContent>
    </IonPage>
  )
}
