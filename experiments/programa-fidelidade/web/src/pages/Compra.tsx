import {
  IonButton,
  IonCard,
  IonCardContent,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonList,
  IonPage,
  IonSelect,
  IonSelectOption,
  IonText,
  IonTitle,
  IonToolbar,
  useIonViewWillEnter,
} from '@ionic/react'
import { useState } from 'react'
import { api, type Parceiro, type ResultadoCompra } from '../api'
import { formatarPontos, formatarReais, normalizarValorDigitado } from '../formatos'
import { useClienteLogado } from '../sessao'

/**
 * Simula o que um parceiro faria pela API: avisar que o cliente comprou e creditar os pontos.
 * Num produto real, isto fica no sistema do parceiro (ou num painel para ele), não no app do cliente.
 */
export default function Compra() {
  const cliente = useClienteLogado()
  const [parceiros, setParceiros] = useState<Parceiro[]>([])
  const [parceiroId, setParceiroId] = useState<number | null>(null)
  const [valorDigitado, setValorDigitado] = useState('')
  const [resultado, setResultado] = useState<ResultadoCompra | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useIonViewWillEnter(() => {
    api.parceiros().then((lista) => {
      setParceiros(lista)
      setParceiroId((atual) => atual ?? lista[0]?.id ?? null)
    })
  })

  const valor = normalizarValorDigitado(valorDigitado)
  const parceiro = parceiros.find((p) => p.id === parceiroId)

  async function registrar() {
    if (!parceiroId || !valor) return
    setErro(null)
    setResultado(null)
    try {
      setResultado(await api.registrarCompra(parceiroId, cliente.id, valor))
      setValorDigitado('')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao registrar a compra')
    }
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Simular compra</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <p>Registre uma compra num parceiro para ganhar pontos.</p>
        <IonList>
          <IonItem>
            <IonSelect
              label="Parceiro"
              labelPlacement="stacked"
              value={parceiroId}
              onIonChange={(e) => setParceiroId(Number(e.detail.value))}
            >
              {parceiros.map((p) => (
                <IonSelectOption key={p.id} value={p.id}>
                  {p.nome}
                </IonSelectOption>
              ))}
            </IonSelect>
          </IonItem>
          <IonItem>
            <IonInput
              label="Valor da compra (R$)"
              labelPlacement="stacked"
              inputmode="decimal"
              placeholder="149,90"
              value={valorDigitado}
              onIonInput={(e) => setValorDigitado(String(e.detail.value ?? ''))}
            />
          </IonItem>
        </IonList>
        {parceiro && (
          <IonText color="medium">
            <p>{parceiro.nome} dá {parceiro.pontosPorReal.replace(/\.?0+$/, '').replace('.', ',')} ponto(s) por real.</p>
          </IonText>
        )}
        {valorDigitado && !valor && (
          <IonText color="danger">
            <p>Valor inválido. Use até 2 casas decimais, ex.: 149,90</p>
          </IonText>
        )}
        <IonButton expand="block" disabled={!parceiroId || !valor} onClick={registrar}>
          Registrar compra
        </IonButton>

        {erro && (
          <IonText color="danger">
            <p>{erro}</p>
          </IonText>
        )}
        {resultado && (
          <IonCard color="success">
            <IonCardContent>
              <h2>+{formatarPontos(resultado.pontosGanhos)} pontos</h2>
              <p>Compra de {formatarReais(resultado.valorCompra)}</p>
              <p>O parceiro paga {formatarReais(resultado.valorDevidoPeloParceiro)} ao programa por esses pontos.</p>
            </IonCardContent>
          </IonCard>
        )}
      </IonContent>
    </IonPage>
  )
}
