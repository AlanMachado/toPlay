import {
  IonButton,
  IonContent,
  IonHeader,
  IonInput,
  IonItem,
  IonList,
  IonPage,
  IonText,
  IonTitle,
  IonToolbar,
} from '@ionic/react'
import { useState } from 'react'
import { api, ErroApi } from '../api'
import { useSessao } from '../sessao'

export default function Entrar() {
  const { entrar } = useSessao()
  const [email, setEmail] = useState('')
  const [nome, setNome] = useState('')
  const [cadastrando, setCadastrando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(false)

  async function continuar() {
    setErro(null)
    setCarregando(true)
    try {
      if (cadastrando) {
        entrar(await api.cadastrarCliente(nome, email))
      } else {
        entrar(await api.buscarCliente(email))
      }
    } catch (e) {
      if (!cadastrando && e instanceof ErroApi && e.status === 404) {
        // E-mail ainda não cadastrado: pede o nome e cria a conta.
        setCadastrando(true)
      } else {
        setErro(e instanceof Error ? e.message : 'Algo deu errado')
      }
    } finally {
      setCarregando(false)
    }
  }

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar color="primary">
          <IonTitle>Fidelidade</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        <h2>{cadastrando ? 'Criar conta' : 'Entrar'}</h2>
        <p>
          {cadastrando
            ? 'Não encontramos esse e-mail. Informe seu nome para criar a conta.'
            : 'Informe seu e-mail para ver seus pontos.'}
        </p>
        <IonList>
          <IonItem>
            <IonInput
              label="E-mail"
              labelPlacement="stacked"
              type="email"
              value={email}
              disabled={cadastrando}
              onIonInput={(e) => setEmail(String(e.detail.value ?? ''))}
            />
          </IonItem>
          {cadastrando && (
            <IonItem>
              <IonInput
                label="Nome"
                labelPlacement="stacked"
                value={nome}
                onIonInput={(e) => setNome(String(e.detail.value ?? ''))}
              />
            </IonItem>
          )}
        </IonList>
        {erro && (
          <IonText color="danger">
            <p>{erro}</p>
          </IonText>
        )}
        <IonButton
          expand="block"
          className="ion-margin-top"
          disabled={carregando || !email || (cadastrando && !nome)}
          onClick={continuar}
        >
          {cadastrando ? 'Criar conta' : 'Continuar'}
        </IonButton>
        {cadastrando && (
          <IonButton expand="block" fill="clear" onClick={() => setCadastrando(false)}>
            Usar outro e-mail
          </IonButton>
        )}
      </IonContent>
    </IonPage>
  )
}
