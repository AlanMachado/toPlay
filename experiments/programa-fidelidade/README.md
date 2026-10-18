# Programa de fidelidade com vouchers

**Objetivo:** programa de pontos em que parceiros informam as compras dos clientes, os clientes
acumulam pontos e trocam esses pontos por vouchers para usar nas lojas parceiras.

| Parte | Tecnologia |
|---|---|
| `backend/` | Kotlin + Spring Boot, Spring Data JPA, Flyway |
| `web/` | React.js + Ionic (TypeScript, Vite): roda no navegador e pode virar app iOS/Android com Capacitor |
| Banco | PostgreSQL (`docker-compose.yml`) |

## Como rodar

Pré-requisitos: **Java 21**, **Node 22** e **Docker** (ou um PostgreSQL 16 local).

```bash
# 1. Banco de dados
docker compose up -d

# 2. Backend (porta 8080). Cria as tabelas e os dados de exemplo ao iniciar.
cd backend
./gradlew bootRun          # no Windows: .\gradlew.bat bootRun

# 3. App web (porta 5173), em outro terminal
cd web
npm install
npm run dev
```

Abra http://localhost:5173, crie uma conta com um e-mail e use a aba **Simular parceiro**
para registrar compras.

> **Vindo do branch anterior (`claude/vigilant-brahmagupta-vzljxa`)?** O schema do banco é
> outro, e o backend não sobe com as tabelas antigas. Apague o banco antes:
> `docker compose down -v` e depois `docker compose up -d`.

## Modelo de dados

```
partner          id, name, active
product          id, name, type, partner_id, points_per_currency_unit, active, valid_until, external_id
client           id, name, email, created_at
voucher          id, product_id, partner_id, active, valid_until, worth_points
voucher_emitted  id, code, voucher_id, client_id, created_at, valid_until, worth_points, product_id, partner_id
transaction      id, client_id, type, product_id, quantity, price, voucher_emitted_id,
                 voucher_points_used, total_points, partner_id, external_order_id, created_at
```

- `product (partner_id, external_id)` é único: a busca de produto vinda do parceiro é sempre por esse par.
- `voucher`: precisa de `product_id` ou `partner_id`. Se tiver produto, ele tem precedência.
- `valid_until` vazio = válido enquanto estiver ativo.
- `transaction` é o livro-razão: as linhas nunca são alteradas.

## Regras de negócio

**Saldo e créditos vêm sempre do histórico.** Não existe campo de saldo:
- saldo do cliente = soma de `transaction.total_points`;
- crédito restante de um voucher emitido = `worth_points` − soma de `voucher_points_used`.

**Pontos de uma compra.** `points_per_currency_unit` é quantos pontos o produto dá por R$ 1,00.
O parceiro envia o **preço total** de cada item. A quantidade é opcional e só fica registrada,
não entra na conta.
- pontos = valor pago × `points_per_currency_unit`, arredondado **para baixo**;
- ex.: R$ 30,00 num produto de 100 pts/R$ = **3.000 pontos**.

**Voucher.** O cliente ativa um modelo de voucher se tiver saldo. Isso debita `worth_points`
(transação `VOUCHER_EMISSION`) e gera um código como `K7QX-M2PA`.
- O voucher guarda só pontos. Quanto ele cobre em reais depende da taxa do produto em que for
  usado: custo do item em pontos = preço × taxa, arredondado **para cima**.
- Crédito suficiente: cobre o item todo, e esse item não gera pontos.
- Crédito insuficiente: cobre o que der (crédito ÷ taxa, arredondado **para baixo** em centavos),
  e só a parte paga gera pontos.
- Num pedido com vários itens, o crédito é aplicado aos itens elegíveis na ordem enviada.

| Compra de R$ 30,00 (100 pts/R$) | Usado do voucher | Coberto | Pago | Pontos gerados |
|---|---|---|---|---|
| sem voucher | 0 | R$ 0,00 | R$ 30,00 | 3.000 |
| voucher com 1.000 pts | 1.000 | R$ 10,00 | R$ 20,00 | 2.000 |
| voucher com 5.000 pts | 3.000 (sobram 2.000) | R$ 30,00 | R$ 0,00 | 0 |

**Validade do voucher emitido.** Na emissão, o voucher copia validade, crédito e escopo do
modelo. Depois disso, desativar ou editar o modelo não afeta quem já emitiu: um voucher emitido
sem data vale para sempre, e um com data vale até ela.

**Só o cliente que emitiu** pode usar o voucher.

**Pedidos do parceiro:**
- O cliente é identificado pelo **e-mail**. Se o e-mail não estiver cadastrado, nada é gravado.
- **Tudo ou nada:** se um item (produto inexistente, inativo ou vencido; preço inválido) ou o
  voucher for inválido, o pedido inteiro é recusado com todos os motivos.
- **Pedido repetido:** o mesmo `(parceiro, externalOrderId)` enviado de novo devolve o que já
  foi gravado (`duplicate: true`) e não credita pontos outra vez.

**Concorrência:** emitir voucher e registrar pedido travam o cliente no banco, e o uso de um
voucher trava o voucher. Testado com requisições simultâneas: o mesmo saldo não é gasto duas
vezes, o mesmo voucher não é usado duas vezes e o mesmo pedido não é creditado duas vezes.

## API

| Quem usa | Método e caminho | O que faz |
|---|---|---|
| App | `POST /api/clients` | Cadastra `{ name, email }` |
| App | `GET /api/clients?email=` | Busca o cliente pelo e-mail ("login") |
| App | `GET /api/clients/{id}/balance` | Saldo, calculado pelo histórico |
| App | `GET /api/clients/{id}/transactions` | Extrato (últimas 50) |
| App | `GET /api/clients/{id}/available-vouchers` | Modelos de voucher ativos, com `affordable` |
| App | `POST /api/clients/{id}/vouchers` | Emite `{ voucherId }` e devolve o código |
| App | `GET /api/clients/{id}/vouchers` | Vouchers emitidos, com crédito restante e status |
| Parceiro | `POST /api/partners/{id}/vouchers/validate` | Valida um voucher para um pedido, sem gravar nada |
| Parceiro | `POST /api/partners/{id}/orders` | Registra o pedido: credita pontos e consome o voucher |
| Admin | `GET`/`POST /api/partners` | Parceiros |
| Admin | `GET`/`POST /api/partners/{id}/products` | Produtos de um parceiro |
| Admin | `GET`/`POST /api/voucher-templates` | Modelos de voucher |

Exemplo de pedido enviado pelo parceiro:

```json
POST /api/partners/3/orders
{
  "externalOrderId": "PED-12345",
  "clientEmail": "ana@exemplo.com",
  "voucherCode": "K7QX-M2PA",
  "items": [
    { "productExternalId": "CAFE-ESP", "quantity": 2, "price": "30.00" },
    { "productExternalId": "PAO-QJO", "price": "12.50" }
  ]
}
```

Valores em dinheiro trafegam como **texto** (`"30.00"`) para o JavaScript não corromper as
casas decimais. O `validate` recebe o mesmo corpo, sem o `externalOrderId`.

## Como está organizado

```
backend/src/main/kotlin/br/loyalty/
├── common/    dinheiro (BigDecimal), validade e tratamento de erros
├── partner/   parceiros e os cadastros de admin
├── product/   produtos
├── client/    clientes, saldo e extrato
├── ledger/    o livro-razão (transaction)
├── voucher/   modelos, emissão, códigos e "meus vouchers"
└── order/     pedidos do parceiro, validação de voucher e cálculo de pontos
backend/src/main/resources/db/migration/   schema (V1) e dados de exemplo (V2)

web/src/
├── api.ts       chamadas ao backend
├── format.ts    formatação de pontos, reais, datas e escopo de voucher
├── session.tsx  cliente logado
└── pages/       SignIn, Balance, Vouchers, PartnerSimulator
```

## Fora do escopo por enquanto

- Autenticação real (o "login" é só por e-mail) e autenticação dos parceiros na API.
- Testes automatizados, cache e filas.
- Painel de admin (os cadastros existem só pela API).
- Empacotar como app nas lojas com Capacitor.
