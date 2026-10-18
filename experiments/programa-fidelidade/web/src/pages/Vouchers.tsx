import {
  IonBadge,
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonContent,
  IonHeader,
  IonLabel,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonText,
  IonTitle,
  IonToolbar,
  useIonAlert,
  useIonToast,
  useIonViewWillEnter,
} from '@ionic/react'
import { useState } from 'react'
import { api, type AvailableVoucher, type EmittedVoucher, type EmittedVoucherStatus } from '../api'
import { describeScope, describeValidity, formatPoints } from '../format'
import { useLoggedClient } from '../session'

type Tab = 'available' | 'mine'

const STATUS: Record<EmittedVoucherStatus, { label: string; color: string }> = {
  ACTIVE: { label: 'Ativo', color: 'success' },
  USED: { label: 'Usado', color: 'medium' },
  EXPIRED: { label: 'Vencido', color: 'danger' },
}

export default function Vouchers() {
  const client = useLoggedClient()
  const [tab, setTab] = useState<Tab>('available')
  const [balance, setBalance] = useState(0)
  const [available, setAvailable] = useState<AvailableVoucher[]>([])
  const [mine, setMine] = useState<EmittedVoucher[]>([])
  // Dois hooks: o alerta de sucesso abre enquanto o de confirmação ainda está fechando,
  // e um mesmo hook só controla um alerta por vez.
  const [showConfirm] = useIonAlert()
  const [showAlert] = useIonAlert()
  const [showToast] = useIonToast()

  async function load() {
    const [b, a, m] = await Promise.all([
      api.balance(client.id),
      api.availableVouchers(client.id),
      api.myVouchers(client.id),
    ])
    setBalance(b.points)
    setAvailable(a)
    setMine(m)
  }

  useIonViewWillEnter(() => {
    load()
  })

  async function emit(voucher: AvailableVoucher) {
    try {
      const emitted = await api.emitVoucher(client.id, voucher.id)
      await load()
      setTab('mine')
      showAlert({
        header: 'Voucher ativado!',
        message: `Seu código é ${emitted.code}. Informe-o na loja para usar ${formatPoints(emitted.worthPoints)} pontos.`,
        buttons: ['OK'],
      })
    } catch (e) {
      showToast({ message: e instanceof Error ? e.message : 'Erro ao ativar', duration: 3000, color: 'danger' })
    }
  }

  function confirmEmit(voucher: AvailableVoucher) {
    showConfirm({
      header: 'Ativar voucher',
      message: `Usar ${formatPoints(voucher.worthPoints)} dos seus pontos para gerar este voucher?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Ativar', handler: () => void emit(voucher) },
      ],
    })
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Vouchers</IonTitle>
          <IonBadge slot="end" color="light" className="ion-margin-end">
            {formatPoints(balance)} pts
          </IonBadge>
        </IonToolbar>
        <IonToolbar>
          <IonSegment value={tab} onIonChange={(e) => setTab(e.detail.value as Tab)}>
            <IonSegmentButton value="available">
              <IonLabel>Disponíveis</IonLabel>
            </IonSegmentButton>
            <IonSegmentButton value="mine">
              <IonLabel>Meus vouchers</IonLabel>
            </IonSegmentButton>
          </IonSegment>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        {tab === 'available' &&
          available.map((v) => (
            <IonCard key={v.id}>
              <IonCardHeader>
                <IonCardTitle>{formatPoints(v.worthPoints)} pontos</IonCardTitle>
                <IonCardSubtitle>{describeScope(v.scope)}</IonCardSubtitle>
              </IonCardHeader>
              <IonCardContent>
                <p>{describeValidity(v.validUntil)}</p>
                <IonButton disabled={!v.affordable} onClick={() => confirmEmit(v)}>
                  {v.affordable ? 'Ativar' : 'Saldo insuficiente'}
                </IonButton>
              </IonCardContent>
            </IonCard>
          ))}

        {tab === 'mine' && mine.length === 0 && (
          <IonText color="medium">
            <p className="ion-padding">Você ainda não ativou nenhum voucher.</p>
          </IonText>
        )}
        {tab === 'mine' &&
          mine.map((v) => (
            <IonCard key={v.id}>
              <IonCardHeader>
                <IonCardTitle className="voucher-code">{v.code}</IonCardTitle>
                <IonCardSubtitle>{describeScope(v.scope)}</IonCardSubtitle>
              </IonCardHeader>
              <IonCardContent>
                <p>
                  <IonBadge color={STATUS[v.status].color}>{STATUS[v.status].label}</IonBadge>{' '}
                  <strong>{formatPoints(v.remainingPoints)}</strong> de {formatPoints(v.worthPoints)} pontos restantes
                </p>
                <p>{describeValidity(v.validUntil)}</p>
              </IonCardContent>
            </IonCard>
          ))}
      </IonContent>
    </IonPage>
  )
}
