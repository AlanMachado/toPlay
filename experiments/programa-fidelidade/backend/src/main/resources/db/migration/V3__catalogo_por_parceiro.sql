-- O custo em pontos de um item deixa de ser gravado: passa a ser calculado a partir do
-- preço do item (em reais) e do preço do ponto do parceiro que oferece o item.
-- Só fica gravado um valor fixo quando é uma promoção da plataforma (pontos_promocionais).

ALTER TABLE catalogo ADD COLUMN parceiro_id BIGINT REFERENCES parceiros (id);
ALTER TABLE catalogo ADD COLUMN preco NUMERIC(19, 2);
ALTER TABLE catalogo RENAME COLUMN custo_pontos TO pontos_promocionais;
ALTER TABLE catalogo ALTER COLUMN pontos_promocionais DROP NOT NULL;

-- Converte os itens de exemplo da V2. Todo item agora pertence a um parceiro.
UPDATE catalogo SET
    nome = 'Vale-compras R$ 25',
    descricao = 'Crédito para usar na Farmácia Saúde',
    parceiro_id = (SELECT id FROM parceiros WHERE nome = 'Farmácia Saúde'),
    preco = 25.00,
    pontos_promocionais = NULL
WHERE nome = 'Vale-presente R$ 25';

UPDATE catalogo SET
    nome = 'Vale-combustível R$ 50',
    descricao = 'Crédito para abastecer no Posto Bom Caminho',
    parceiro_id = (SELECT id FROM parceiros WHERE nome = 'Posto Bom Caminho'),
    preco = 50.00,
    pontos_promocionais = NULL
WHERE nome = 'Vale-presente R$ 50';

-- Exemplo de promoção: o café custaria 229 pontos, mas a plataforma fixou em 150.
UPDATE catalogo SET
    parceiro_id = (SELECT id FROM parceiros WHERE nome = 'Café da Esquina'),
    preco = 8.00,
    pontos_promocionais = 150
WHERE nome = 'Café grátis';

UPDATE catalogo SET
    parceiro_id = (SELECT id FROM parceiros WHERE nome = 'Posto Bom Caminho'),
    preco = 45.00,
    pontos_promocionais = NULL
WHERE nome = 'Lavagem completa';

ALTER TABLE catalogo ALTER COLUMN parceiro_id SET NOT NULL;
ALTER TABLE catalogo ALTER COLUMN preco SET NOT NULL;
ALTER TABLE catalogo ADD CONSTRAINT catalogo_preco_positivo CHECK (preco > 0);
