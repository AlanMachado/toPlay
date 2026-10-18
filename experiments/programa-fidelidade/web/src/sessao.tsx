import { createContext, useContext, useState, type ReactNode } from 'react'
import type { Cliente } from './api'

// "Login" provisório: guardamos o cliente no navegador. Ainda não há senha nem token.
const CHAVE = 'fidelidade.cliente'

type Sessao = {
  cliente: Cliente | null
  entrar: (cliente: Cliente) => void
  sair: () => void
}

const SessaoContexto = createContext<Sessao | null>(null)

function lerClienteSalvo(): Cliente | null {
  try {
    const salvo = localStorage.getItem(CHAVE)
    return salvo ? (JSON.parse(salvo) as Cliente) : null
  } catch {
    return null
  }
}

export function SessaoProvider({ children }: { children: ReactNode }) {
  const [cliente, setCliente] = useState<Cliente | null>(lerClienteSalvo)

  const entrar = (novo: Cliente) => {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(novo))
    } catch {
      // Sem localStorage (ex.: navegação privada): a sessão vale só enquanto a página estiver aberta.
    }
    setCliente(novo)
  }

  const sair = () => {
    try {
      localStorage.removeItem(CHAVE)
    } catch {
      // ignora
    }
    setCliente(null)
  }

  return <SessaoContexto.Provider value={{ cliente, entrar, sair }}>{children}</SessaoContexto.Provider>
}

export function useSessao(): Sessao {
  const sessao = useContext(SessaoContexto)
  if (!sessao) throw new Error('useSessao precisa estar dentro de <SessaoProvider>')
  return sessao
}

/** Para telas que só aparecem com alguém logado. */
export function useClienteLogado(): Cliente {
  const { cliente } = useSessao()
  if (!cliente) throw new Error('Nenhum cliente logado')
  return cliente
}
