-- Dados de exemplo para testar o app localmente.

INSERT INTO parceiros (nome, preco_por_ponto, pontos_por_real) VALUES
    ('Posto Bom Caminho', 0.0300, 2.0000),
    ('Farmácia Saúde',    0.0250, 1.5000),
    ('Café da Esquina',   0.0350, 3.0000);

INSERT INTO catalogo (nome, descricao, custo_pontos) VALUES
    ('Vale-presente R$ 25',  'Crédito para usar em qualquer parceiro', 1000),
    ('Vale-presente R$ 50',  'Crédito para usar em qualquer parceiro', 1900),
    ('Café grátis',          'Um café expresso no Café da Esquina',     300),
    ('Lavagem completa',     'Lavagem no Posto Bom Caminho',           1500);
