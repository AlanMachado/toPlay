-- Pontos são sempre BIGINT (inteiros). Dinheiro é sempre NUMERIC (decimal exato).

CREATE TABLE clientes (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE parceiros (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120)   NOT NULL,
    -- Quanto o parceiro paga por ponto emitido (ex.: 0.0300 = R$ 30 por mil pontos).
    preco_por_ponto  NUMERIC(19, 4) NOT NULL CHECK (preco_por_ponto > 0),
    -- Quantos pontos o cliente ganha por real gasto no parceiro.
    pontos_por_real  NUMERIC(19, 4) NOT NULL CHECK (pontos_por_real > 0),
    criado_em        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE catalogo (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    descricao     VARCHAR(400),
    custo_pontos  BIGINT       NOT NULL CHECK (custo_pontos > 0),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE resgates (
    id           BIGSERIAL PRIMARY KEY,
    cliente_id   BIGINT      NOT NULL REFERENCES clientes (id),
    item_id      BIGINT      NOT NULL REFERENCES catalogo (id),
    pontos       BIGINT      NOT NULL CHECK (pontos > 0),
    status       VARCHAR(20) NOT NULL,
    criado_em    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- O "livro-razão": toda movimentação de pontos é um lançamento imutável.
-- Saldo do cliente = SUM(pontos). Créditos são positivos, resgates negativos.
-- Correções são feitas com um novo lançamento (estorno), nunca com UPDATE.
CREATE TABLE lancamentos (
    id            BIGSERIAL PRIMARY KEY,
    cliente_id    BIGINT         NOT NULL REFERENCES clientes (id),
    tipo          VARCHAR(20)    NOT NULL,
    pontos        BIGINT         NOT NULL CHECK (pontos <> 0),
    -- Valor em reais associado (ex.: quanto o parceiro deve por estes pontos).
    valor         NUMERIC(19, 2),
    parceiro_id   BIGINT         REFERENCES parceiros (id),
    resgate_id    BIGINT         REFERENCES resgates (id),
    descricao     VARCHAR(200)   NOT NULL,
    criado_em     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_lancamentos_cliente ON lancamentos (cliente_id, criado_em DESC);
