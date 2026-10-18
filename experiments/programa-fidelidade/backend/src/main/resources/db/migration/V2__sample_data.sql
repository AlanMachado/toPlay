-- Dados de exemplo para testar o app localmente.

INSERT INTO partner (id, name) VALUES
    (1, 'Posto Bom Caminho'),
    (2, 'Farmácia Saúde'),
    (3, 'Café da Esquina');
SELECT setval('partner_id_seq', 3);

INSERT INTO product (id, name, type, partner_id, points_per_currency_unit, external_id, valid_until) VALUES
    (1, 'Gasolina comum (litro)', 'combustível', 1, 2.0000,   'GAS-COMUM', NULL),
    (2, 'Lavagem completa',       'serviço',     1, 5.0000,   'LAV-001',   NULL),
    (3, 'Vitamina C 30 cápsulas', 'medicamento', 2, 10.0000,  '789100001', NULL),
    (4, 'Protetor solar FPS 50',  'cosmético',   2, 10.0000,  '789100002', NULL),
    (5, 'Café expresso',          'bebida',      3, 100.0000, 'CAFE-ESP',  NULL),
    (6, 'Pão de queijo',          'comida',      3, 100.0000, 'PAO-QJO',   NULL),
    -- Produto em promoção até o fim de 2027.
    (7, 'Combo café da manhã',    'combo',       3, 150.0000, 'COMBO-CM',  '2027-12-31 23:59:59-03');
SELECT setval('product_id_seq', 7);

INSERT INTO voucher (product_id, partner_id, valid_until, worth_points) VALUES
    -- Vale para qualquer produto do parceiro.
    (NULL, 3, NULL, 500),
    (NULL, 3, NULL, 1500),
    (NULL, 2, NULL, 300),
    (NULL, 1, '2027-06-30 23:59:59-03', 200),
    -- Vale só para um produto específico.
    (2,    NULL, NULL, 250);
