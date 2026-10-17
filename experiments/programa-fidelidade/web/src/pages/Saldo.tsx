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
import { api, type Lancamento } from '../api'
import { formatarData, formatarPontos } from '../formatos'
import { useClienteLogado, useSessao } from '../sessao'

export default function Saldo() {
  const cliente = useClienteLogado()
  const { sair } = useSessao()
  const [pontos, setPontos] = useState<number | null>(null)
  const [extrato, setExtrato] = useState<Lancamento[]>([])
  const [erro, setErro] = useState<string | null>(null)

  async function carregar() {
    try {
      const [saldo, lancamentos] = await Promise.all([api.saldo(cliente.id), api.extrato(cliente.id)])
      setPontos(saldo.pontos)
      setExtrato(lancamentos)
      setErro(null)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível carregar o saldo')
    }
  }

  // Recarrega sempre que a aba aparece (ex.: depois de um resgate ou de uma compra).
  useIonViewWillEnter(() => {
    carregar()
  })

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Olá, {cliente.nome.split(' ')[0]}</IonTitle>
          <IonButtons slot="end">
            <IonButton onClick={sair}>Sair</IonButton>
          </IonButtons>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonRefresher slot="fixed" onIonRefresh={(e) => carregar().finally(() => e.detail.complete())}>
          <IonRefresherContent />
        </IonRefresher>

        <IonCard className="saldo-cartao">
          <IonNote>Seu saldo</IonNote>
          <div className="saldo-valor">{pontos === null ? '—' : formatarPontos(pontos)}</div>
          <IonNote>pontos</IonNote>
        </IonCard>

        {erro && (
          <IonText color="danger">
            <p className="ion-padding-horizontal">{erro}</p>
          </IonText>
        )}

        <IonList>
          <IonListHeader>
            <IonLabel>Extrato</IonLabel>
          </IonListHeader>
          {extrato.length === 0 && (
            <IonItem lines="none">
              <IonLabel color="medium">Nenhuma movimentação ainda. Use "Simular compra" para ganhar pontos.</IonLabel>
            </IonItem>
          )}
          {extrato.map((l) => (
            <IonItem key={l.id}>
              <IonLabel>
                <h3>{l.descricao}</h3>
                <p>{formatarData(l.criadoEm)}</p>
              </IonLabel>
              <span slot="end" className={l.pontos > 0 ? 'pontos-positivos' : 'pontos-negativos'}>
                {l.pontos > 0 ? '+' : ''}
                {formatarPontos(l.pontos)}
              </span>
            </IonItem>
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  )
}
