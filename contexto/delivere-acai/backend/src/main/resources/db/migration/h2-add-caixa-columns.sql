-- Adiciona colunas reabertura, valor_fechamento e valor_retirada na tabela caixa.
-- Execute no H2 Console (http://localhost:8080/h2-console) se aparecer erro de coluna não encontrada.
-- Se der erro "Column already exists", as colunas já foram criadas; pode ignorar.

ALTER TABLE caixa ADD COLUMN valor_fechamento DECIMAL(12,2);
ALTER TABLE caixa ADD COLUMN reabertura BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE caixa ADD COLUMN valor_retirada DECIMAL(12,2);
