import { IonApp, IonIcon, IonLabel, IonRouterOutlet, IonTabBar, IonTabButton, IonTabs } from '@ionic/react'
import { IonReactRouter } from '@ionic/react-router'
import { cartOutline, giftOutline, walletOutline } from 'ionicons/icons'
import { Navigate, Route } from 'react-router-dom'
import Catalogo from './pages/Catalogo'
import Compra from './pages/Compra'
import Entrar from './pages/Entrar'
import Saldo from './pages/Saldo'
import { SessaoProvider, useSessao } from './sessao'

function Abas() {
  return (
    <IonReactRouter>
      <IonTabs>
        <IonRouterOutlet>
          <Route path="/saldo" element={<Saldo />} />
          <Route path="/catalogo" element={<Catalogo />} />
          <Route path="/compra" element={<Compra />} />
          <Route path="*" element={<Navigate to="/saldo" replace />} />
        </IonRouterOutlet>
        <IonTabBar slot="bottom">
          <IonTabButton tab="saldo" href="/saldo">
            <IonIcon icon={walletOutline} />
            <IonLabel>Saldo</IonLabel>
          </IonTabButton>
          <IonTabButton tab="catalogo" href="/catalogo">
            <IonIcon icon={giftOutline} />
            <IonLabel>Resgatar</IonLabel>
          </IonTabButton>
          <IonTabButton tab="compra" href="/compra">
            <IonIcon icon={cartOutline} />
            <IonLabel>Simular compra</IonLabel>
          </IonTabButton>
        </IonTabBar>
      </IonTabs>
    </IonReactRouter>
  )
}

function Conteudo() {
  const { cliente } = useSessao()
  return cliente ? <Abas /> : <Entrar />
}

export default function App() {
  return (
    <IonApp>
      <SessaoProvider>
        <Conteudo />
      </SessaoProvider>
    </IonApp>
  )
}
