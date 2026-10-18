import { createContext, useContext, useState, type ReactNode } from 'react'
import type { Client } from './api'

// "Login" provisório: guardamos o cliente no navegador. Ainda não há senha nem token.
const STORAGE_KEY = 'loyalty.client'

type Session = {
  client: Client | null
  signIn: (client: Client) => void
  signOut: () => void
}

const SessionContext = createContext<Session | null>(null)

function readSavedClient(): Client | null {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? (JSON.parse(saved) as Client) : null
  } catch {
    return null
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [client, setClient] = useState<Client | null>(readSavedClient)

  const signIn = (newClient: Client) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newClient))
    } catch {
      // Sem localStorage (ex.: navegação privada): a sessão vale só enquanto a página estiver aberta.
    }
    setClient(newClient)
  }

  const signOut = () => {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignora
    }
    setClient(null)
  }

  return <SessionContext.Provider value={{ client, signIn, signOut }}>{children}</SessionContext.Provider>
}

export function useSession(): Session {
  const session = useContext(SessionContext)
  if (!session) throw new Error('useSession precisa estar dentro de <SessionProvider>')
  return session
}

/** Para telas que só aparecem com alguém logado. */
export function useLoggedClient(): Client {
  const { client } = useSession()
  if (!client) throw new Error('Nenhum cliente logado')
  return client
}
