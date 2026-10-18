import { IonApp, IonIcon, IonLabel, IonRouterOutlet, IonTabBar, IonTabButton, IonTabs } from '@ionic/react'
import { IonReactRouter } from '@ionic/react-router'
import { storefrontOutline, ticketOutline, walletOutline } from 'ionicons/icons'
import { Navigate, Route } from 'react-router-dom'
import Balance from './pages/Balance'
import PartnerSimulator from './pages/PartnerSimulator'
import SignIn from './pages/SignIn'
import Vouchers from './pages/Vouchers'
import { SessionProvider, useSession } from './session'

function Tabs() {
  return (
    <IonReactRouter>
      <IonTabs>
        <IonRouterOutlet>
          <Route path="/balance" element={<Balance />} />
          <Route path="/vouchers" element={<Vouchers />} />
          <Route path="/partner" element={<PartnerSimulator />} />
          <Route path="*" element={<Navigate to="/balance" replace />} />
        </IonRouterOutlet>
        <IonTabBar slot="bottom">
          <IonTabButton tab="balance" href="/balance">
            <IonIcon icon={walletOutline} />
            <IonLabel>Saldo</IonLabel>
          </IonTabButton>
          <IonTabButton tab="vouchers" href="/vouchers">
            <IonIcon icon={ticketOutline} />
            <IonLabel>Vouchers</IonLabel>
          </IonTabButton>
          <IonTabButton tab="partner" href="/partner">
            <IonIcon icon={storefrontOutline} />
            <IonLabel>Simular parceiro</IonLabel>
          </IonTabButton>
        </IonTabBar>
      </IonTabs>
    </IonReactRouter>
  )
}

function Content() {
  const { client } = useSession()
  return client ? <Tabs /> : <SignIn />
}

export default function App() {
  return (
    <IonApp>
      <SessionProvider>
        <Content />
      </SessionProvider>
    </IonApp>
  )
}
