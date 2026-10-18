-- Pontos são sempre BIGINT (inteiros). Dinheiro é sempre NUMERIC (decimal exato).

CREATE TABLE partner (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(120) NOT NULL,
    active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE product (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(160)   NOT NULL,
    type                       VARCHAR(60),
    partner_id                 BIGINT         NOT NULL REFERENCES partner (id),
    -- Pontos por unidade de moeda gasta (ex.: 100 = 100 pontos por R$ 1,00).
    -- Vale nos dois sentidos: para gerar pontos e para calcular o custo em pontos ao usar voucher.
    points_per_currency_unit   NUMERIC(19, 4) NOT NULL CHECK (points_per_currency_unit > 0),
    active                     BOOLEAN        NOT NULL DEFAULT TRUE,
    -- Nulo = sempre válido enquanto ativo.
    valid_until                TIMESTAMPTZ,
    -- Id do produto na plataforma do parceiro. A busca é sempre pelo par (partner_id, external_id).
    external_id                VARCHAR(120)   NOT NULL,
    CONSTRAINT product_external_id_unique UNIQUE (partner_id, external_id)
);

CREATE TABLE client (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Modelo de voucher que o cliente pode ativar.
CREATE TABLE voucher (
    id            BIGSERIAL PRIMARY KEY,
    -- Se product_id estiver preenchido, ele tem precedência: o voucher só vale para esse produto.
    -- Se não, vale para qualquer produto do partner_id.
    product_id    BIGINT  REFERENCES product (id),
    partner_id    BIGINT  REFERENCES partner (id),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    valid_until   TIMESTAMPTZ,
    -- Custo para emitir e crédito do voucher, ambos em pontos.
    worth_points  BIGINT  NOT NULL CHECK (worth_points > 0),
    CONSTRAINT voucher_scope_required CHECK (product_id IS NOT NULL OR partner_id IS NOT NULL)
);

-- Voucher ativado por um cliente. Validade, crédito e escopo são copiados do modelo na emissão:
-- mudanças posteriores no modelo (inclusive desativá-lo) não afetam vouchers já emitidos.
CREATE TABLE voucher_emitted (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20) NOT NULL UNIQUE,
    voucher_id    BIGINT      NOT NULL REFERENCES voucher (id),
    client_id     BIGINT      NOT NULL REFERENCES client (id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until   TIMESTAMPTZ,
    worth_points  BIGINT      NOT NULL CHECK (worth_points > 0),
    product_id    BIGINT      REFERENCES product (id),
    partner_id    BIGINT      REFERENCES partner (id),
    CONSTRAINT voucher_emitted_scope_required CHECK (product_id IS NOT NULL OR partner_id IS NOT NULL)
);

CREATE INDEX idx_voucher_emitted_client ON voucher_emitted (client_id);

-- O livro-razão de pontos. Linhas imutáveis: nunca há UPDATE.
-- Saldo do cliente = SUM(total_points).
-- Crédito restante de um voucher emitido = worth_points - SUM(voucher_points_used).
CREATE TABLE transaction (
    id                   BIGSERIAL PRIMARY KEY,
    client_id            BIGINT         NOT NULL REFERENCES client (id),
    type                 VARCHAR(20)    NOT NULL,
    product_id           BIGINT         REFERENCES product (id),
    quantity             INTEGER        CHECK (quantity > 0),
    -- Preço total informado pelo parceiro. Não é preço de catálogo.
    price                NUMERIC(19, 2) CHECK (price > 0),
    voucher_emitted_id   BIGINT         REFERENCES voucher_emitted (id),
    voucher_points_used  BIGINT         NOT NULL DEFAULT 0 CHECK (voucher_points_used >= 0),
    total_points         BIGINT         NOT NULL,
    partner_id           BIGINT         REFERENCES partner (id),
    -- Id do pedido no parceiro: evita creditar duas vezes o mesmo pedido.
    external_order_id    VARCHAR(120),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT transaction_purchase_fields CHECK (
        type <> 'PURCHASE' OR (product_id IS NOT NULL AND price IS NOT NULL
            AND partner_id IS NOT NULL AND external_order_id IS NOT NULL AND total_points >= 0)
    ),
    CONSTRAINT transaction_emission_fields CHECK (
        type <> 'VOUCHER_EMISSION' OR (voucher_emitted_id IS NOT NULL AND total_points < 0)
    )
);

CREATE INDEX idx_transaction_client ON transaction (client_id, created_at DESC);
CREATE INDEX idx_transaction_voucher_emitted ON transaction (voucher_emitted_id);
CREATE INDEX idx_transaction_order ON transaction (partner_id, external_order_id);
