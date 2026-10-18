import {
  IonBadge,
  IonButton,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonNote,
  IonPage,
  IonTitle,
  IonToolbar,
  useIonAlert,
  useIonToast,
  useIonViewWillEnter,
} from '@ionic/react'
import { useState } from 'react'
import { api, type ItemCatalogo } from '../api'
import { formatarPontos, formatarReais } from '../formatos'
import { useClienteLogado } from '../sessao'

export default function Catalogo() {
  const cliente = useClienteLogado()
  const [itens, setItens] = useState<ItemCatalogo[]>([])
  const [saldo, setSaldo] = useState(0)
  const [mostrarAlerta] = useIonAlert()
  const [mostrarToast] = useIonToast()

  async function carregar() {
    const [catalogo, s] = await Promise.all([api.catalogo(), api.saldo(cliente.id)])
    setItens(catalogo)
    setSaldo(s.pontos)
  }

  useIonViewWillEnter(() => {
    carregar()
  })

  async function resgatar(item: ItemCatalogo) {
    try {
      const resultado = await api.resgatar(cliente.id, item.id)
      setSaldo(resultado.saldoRestante)
      mostrarToast({
        message: `${item.nome} resgatado! Saldo: ${formatarPontos(resultado.saldoRestante)} pontos`,
        duration: 2500,
        color: 'success',
      })
    } catch (e) {
      mostrarToast({ message: e instanceof Error ? e.message : 'Erro no resgate', duration: 3000, color: 'danger' })
    }
  }

  function confirmar(item: ItemCatalogo) {
    mostrarAlerta({
      header: 'Confirmar resgate',
      message: `Usar ${formatarPontos(item.custoPontos)} pontos em "${item.nome}"?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Resgatar', handler: () => void resgatar(item) },
      ],
    })
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Resgatar</IonTitle>
          <IonBadge slot="end" color="light" className="ion-margin-end">
            {formatarPontos(saldo)} pts
          </IonBadge>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonList>
          {itens.map((item) => (
            <IonItem key={item.id}>
              <IonLabel>
                <h3>{item.nome}</h3>
                <p>
                  {item.parceiroNome} · {formatarReais(item.preco)}
                </p>
                {item.descricao && <p>{item.descricao}</p>}
                <IonNote>
                  {item.emPromocao && (
                    <>
                      <IonBadge color="warning">Promoção</IonBadge>{' '}
                      <s>{formatarPontos(item.custoPontosSemPromocao)}</s>{' '}
                    </>
                  )}
                  <strong>{formatarPontos(item.custoPontos)} pontos</strong>
                </IonNote>
              </IonLabel>
              <IonButton slot="end" disabled={saldo < item.custoPontos} onClick={() => confirmar(item)}>
                Resgatar
              </IonButton>
            </IonItem>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  )
}
