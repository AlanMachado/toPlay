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
import { api, ApiError } from '../api'
import { useSession } from '../session'

export default function SignIn() {
  const { signIn } = useSession()
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit() {
    setError(null)
    setLoading(true)
    try {
      signIn(creating ? await api.createClient(name, email) : await api.findClient(email))
    } catch (e) {
      if (!creating && e instanceof ApiError && e.status === 404) {
        // E-mail ainda não cadastrado: pede o nome e cria a conta.
        setCreating(true)
      } else {
        setError(e instanceof Error ? e.message : 'Algo deu errado')
      }
    } finally {
      setLoading(false)
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
        <h2>{creating ? 'Criar conta' : 'Entrar'}</h2>
        <p>
          {creating
            ? 'Não encontramos esse e-mail. Informe seu nome para entrar no programa.'
            : 'Use o mesmo e-mail que você usa nas lojas parceiras.'}
        </p>
        <IonList>
          <IonItem>
            <IonInput
              label="E-mail"
              labelPlacement="stacked"
              type="email"
              value={email}
              disabled={creating}
              onIonInput={(e) => setEmail(String(e.detail.value ?? ''))}
            />
          </IonItem>
          {creating && (
            <IonItem>
              <IonInput
                label="Nome"
                labelPlacement="stacked"
                value={name}
                onIonInput={(e) => setName(String(e.detail.value ?? ''))}
              />
            </IonItem>
          )}
        </IonList>
        {error && (
          <IonText color="danger">
            <p>{error}</p>
          </IonText>
        )}
        <IonButton
          expand="block"
          className="ion-margin-top"
          disabled={loading || !email || (creating && !name)}
          onClick={submit}
        >
          {creating ? 'Criar conta' : 'Continuar'}
        </IonButton>
        {creating && (
          <IonButton expand="block" fill="clear" onClick={() => setCreating(false)}>
            Usar outro e-mail
          </IonButton>
        )}
      </IonContent>
    </IonPage>
  )
}
