-- Espetinho Jurema — reset só do cardápio e do que referencia produto (H2 / schema atual JPA).
-- Não apaga pedidos, mesas, usuários, clientes, caixa nem configuração.
-- Ordem respeita FKs: itens e pagamentos antes de produtos; movimentos de estoque antes de produtos.
--
-- Como usar: H2 Console (http://localhost:9090/h2-console), JDBC URL igual à do application.yml
-- (ex.: jdbc:h2:file:./data/espetinho), depois colar e executar. Em seguida reinicie a API: o
-- DataInitializer recria categorias e produtos oficiais quando categorias.count = 0 e grava versao_catalogo_seed.
--
-- Em operação normal não é necessário: na subida, se versao_catalogo_seed < app.catalogo.versao-seed-oficial,
-- o backend já limpa e reaplica o cardápio sozinho. Use este script só para emergência ou banco travado.
--
-- Opcional: APP_CATALOGO_FORCAR_RESEED=true força o mesmo a cada startup (só dev).

DELETE FROM itens_pedido;
DELETE FROM pedidos_pagamentos;
DELETE FROM movimentos_estoque;
DELETE FROM produtos;
DELETE FROM categorias;
