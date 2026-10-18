# Programa de fidelidade

**Objetivo:** primeiro esqueleto de um programa de pontos próprio. Parceiros creditam pontos
quando o cliente compra, e o cliente consulta saldo/extrato e resgata itens do catálogo.

| Parte | Tecnologia |
|---|---|
| `backend/` | Kotlin + Spring Boot, Spring Data JPA, Flyway |
| `web/` | React.js + Ionic (TypeScript, Vite), roda no navegador e vira app iOS/Android com Capacitor depois |
| Banco | PostgreSQL (`docker-compose.yml`) |

## Como rodar

Pré-requisitos: **Java 21**, **Node 22** e **Docker** (ou um PostgreSQL 16 local).

```bash
# 1. Banco de dados
docker compose up -d

# 2. Backend (porta 8080). Na primeira vez baixa as dependências e cria as tabelas.
cd backend
./gradlew bootRun

# 3. App web (porta 5173), em outro terminal
cd web
npm install
npm run dev
```

Abra http://localhost:5173, informe um e-mail para criar a conta e use a aba
**Simular compra** para ganhar pontos.

Para testar no celular na mesma rede Wi-Fi: `npm run dev -- --host` e abra o endereço
"Network" que o Vite mostrar.

## Como está organizado

```
backend/src/main/kotlin/br/fidelidade/
├── comum/        regras de dinheiro (BigDecimal, arredondamento) e tratamento de erros
├── clientes/     cadastro, "login" por e-mail, saldo e extrato
├── parceiros/    parceiros e registro de compras (gera os pontos)
├── lancamentos/  o livro-razão de pontos
├── catalogo/     itens resgatáveis
└── resgates/     troca de pontos por itens
backend/src/main/resources/db/migration/   tabelas (V1) e dados de exemplo (V2)

web/src/
├── api.ts        chamadas ao backend
├── formatos.ts   formatação de pontos, reais e datas
├── sessao.tsx    cliente logado
└── pages/        Entrar, Saldo, Catalogo (resgate), Compra (simulação)
```

## API

| Método | Caminho | O que faz |
|---|---|---|
| `POST` | `/api/clientes` | Cadastra `{ nome, email }` |
| `GET` | `/api/clientes?email=` | Busca o cliente pelo e-mail ("login") |
| `GET` | `/api/clientes/{id}/saldo` | Saldo em pontos |
| `GET` | `/api/clientes/{id}/extrato` | Últimos 50 lançamentos |
| `GET` | `/api/parceiros` | Lista parceiros |
| `POST` | `/api/parceiros/{id}/compras` | Registra compra `{ clienteId, valorCompra: "149.90" }` e credita pontos |
| `GET` | `/api/catalogo` | Itens resgatáveis, com o custo em pontos já calculado |
| `POST` | `/api/resgates` | Resgata `{ clienteId, itemId }` |

## Regras de negócio e de dinheiro

- **Saldo = soma dos lançamentos.** Não existe campo "saldo" editável. Cada crédito ou resgate
  é uma linha imutável em `lancamentos`, e correções serão feitas com estorno.
- **Pontos são inteiros** (`BIGINT`/`Long`). **Dinheiro é decimal exato** (`NUMERIC`/`BigDecimal`),
  nunca `Double`.
- **Pontos ganhos** = valor da compra × pontos por real do parceiro, **arredondado para baixo**.
  Ex.: R$ 149,90 × 2 = 299,8 → 299 pontos.
- **Valor devido pelo parceiro** = pontos × preço por ponto, com **arredondamento bancário**
  (`HALF_EVEN`) em 2 casas. Ex.: 3.703 × R$ 0,035 = 129,605 → R$ 129,60.
- **Custo de um item em pontos não é gravado:** cada item tem um preço em reais e pertence a
  um parceiro, e o custo é calculado na hora como preço ÷ preço do ponto do parceiro,
  **arredondado para cima**. Ex.: R$ 50,00 ÷ R$ 0,03 = 1.666,7 → 1.667 pontos. Se o parceiro
  mudar o preço do ponto, o catálogo inteiro dele se ajusta sozinho.
- **Promoção da plataforma:** um item pode ter `pontos_promocionais`, um custo fixo que
  substitui o calculado. O app mostra o preço normal riscado.
- **O resgate grava os pontos cobrados** e o valor em reais do item (o que a plataforma
  repassa ao parceiro), então mudanças de preço futuras não alteram resgates passados.
- **Valores em dinheiro trafegam no JSON como texto** (`"149.90"`), e o front nunca faz
  conta com dinheiro: só exibe o que o backend calculou.
- **Resgates travam o cliente** no banco (`SELECT ... FOR UPDATE`), então dois resgates
  simultâneos não conseguem gastar o mesmo saldo.

## Fora do escopo por enquanto

- Autenticação real (o "login" é só por e-mail, sem senha).
- Vencimento de pontos, filas, cache e testes automatizados.
- Painel do parceiro/admin. A aba "Simular compra" faz o papel do parceiro.
- Pagamentos (Pix, cartão) e, por último, cripto.
- Empacotar como app nas lojas com Capacitor.

## Aprendizados

- Entidades JPA em Kotlin ficam "abertas" (plugin `allOpen`), então o Kotlin não faz smart cast
  em `entidade.id`. Use o id que você já tem numa variável local em vez de `entidade.id!!`.
- O Ionic 9 usa React Router v6 (`<Route path element={...} />`).
