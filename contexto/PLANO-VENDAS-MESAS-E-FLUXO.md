# Plano de produto e implementação — Espetinho Jurema (foco mesas e operação)

Documento de alinhamento. **Versão 1.1** — decisões de receita, pedidos na mesa e estoque simples incorporadas.

---

## 1. Escopo excluído ou adiado (por enquanto)

| Item | Decisão |
|------|---------|
| **Delivery** | **Oculto** na navegação; **não evoluir** neste ciclo. |
| **Balcão** | **Removido/oculto** da navegação. Operação concentrada em **mesas**. |
| **Financeiro amplo** | **Sem** contas a pagar / contas a receber. |
| **Integrações iFood/WhatsApp** | Fora do escopo atual. |

---

## 2. O que entra neste ciclo (resumo)

1. **Vendas por mesa** alinhadas às **imagens de referência** (fluxo completo até pagamento).
2. **Pedidos** no mesmo padrão visual/funcional das referências: **aparecem no contexto da mesa** (lista/cartões + detalhe, como no legado), não como módulo solto sem nexo.
3. **Alerta mobile → PC (atendimento) → OK → comanda** para o churrasqueiro.
4. **Perfis:** atendimento (PC), garçom, churrasqueiro.
5. **Estoque simples** (ver §7): entrada por produto, saída na venda, opção obrigatória ou não, ajuste para zerar/organizar depois.
6. **Resumo financeiro:** por ora **“lucro” = receita** (valor vendido); **totais por forma de pagamento**; relatório por período + imprimir. *Margem real (custo) fica para evolução futura.*

---

## 3. Papéis e dispositivos (modelo operacional)

| Perfil | Uso típico | Responsabilidade principal |
|--------|------------|----------------------------|
| **Atendimento** | Computador (web) | Alertas de mesa aberta no mobile, **OK** → comanda; apoio ao fechamento. |
| **Garçom** | Celular | Abrir mesa, lançar pedidos. |
| **Churrasqueiro** | Celular / estação | Conforme permissão; comandas impressas. |

**Autenticação:** login + `tipoPerfil` (`ATENDIMENTO`, `GARCOM`, `CHURRASQUEIRO`).

---

## 4. Módulos de interface (visíveis)

- **Início** — dashboard (receita no período + formas de pagamento — ver §6).
- **Mesas** — grade, filtros, **detalhe da mesa com pedidos/itens** no padrão das imagens.
- **Pedidos** — **lista/controle como nas referências** (filtros, cards, finalizados, intervalo de data quando aplicável), sempre com **nexo mesa/tipo** visível; não é tela órfã.
- **Cadastros** — produtos, categorias, clientes, usuários, **flag estoque obrigatório**.
- **Estoque** — entrada; ajuste/zerar saldo quando necessário.
- **Resumo / Relatórios** — período + impressão.

**Ocultos:** Delivery, Balcão.

---

## 5. Fluxo técnico — alerta + comanda

1. Mobile **abre/inicia mesa** (evento definido).
2. WebSocket notifica **atendimento**.
3. **OK** → comanda cozinha (impressão) + idempotência.

---

## 6. Resumo financeiro e dashboard (decisão atual)

| Conceito | Comportamento **agora** |
|----------|-------------------------|
| **“Lucro” no painel** | Tratar como **receita** = soma do **valor vendido** no período (itens pagos/fechados conforme regra de negócio). **Não** calcular receita − custo nesta versão. |
| **Evolução futura** | Introduzir custo/margem real quando o cliente quiser refinar. |
| **Por forma de pagamento** | Totais em dinheiro, Pix, débito, crédito, etc. |
| **Relatório** | Consulta por período + **imprimir** (HTML/PDF/texto). |

---

## 7. Estoque — modelo simples (decisão atual)

### 7.1 Ideia geral

- O dono dá **entrada**: “espetinho X”, **quantidade** que entrou (saldo aumenta).
- **Durante as vendas**, cada item vendido **dá saída** (decrementa saldo do produto), de forma automática.

### 7.2 Opção global: estoque obrigatório

| Configuração | Comportamento na venda |
|--------------|-------------------------|
| **Obrigatório ligado** | Se a quantidade a vender **zerar ou ultrapassar** o disponível → **bloquear** (ou não permitir confirmar) e **avisar** que não há estoque suficiente. |
| **Obrigatório desligado** | **Deixa passar** mesmo ficando **negativo** (venda sem bloqueio por saldo). |

### 7.3 Organizar depois (trabalhou no negativo)

- Oferecer ação **ajustar / zerar saldo** (ou “corrigir estoque”) no produto ou na tela de estoque, para quando o cliente quiser **alinhar o sistema** com a realidade física após operar sem controle rígido.

### 7.4 Escopo de tela

- **Entrada** de estoque (produto, quantidade, opcionalmente data/ref.).
- **Saldo atual** por produto visível onde fizer sentido (cadastro ou estoque).
- **Ajuste manual** (incluindo zerar) para regularização.

---

## 8. Fases de implementação (atualizadas)

Cada fase fecha com teste **celular + PC** quando aplicável.

### Fase A — Identidade e perfis
- Usuários, perfis, auth, guards (API + Angular).

### Fase B — Navegação
- Ocultar Delivery e Balcão.

### Fase C — Mesas e vendas (núcleo)
- UI como referência: **pedidos vinculados à mesa** (lista/cards + detalhe).
- Itens, observações, **pagamento** (formas, parcial, troco).
- Cupom de conferência quando aplicável.

### Fase D — Mobile → alerta PC → comanda
- WebSocket + fila de alertas (atendimento) + impressão comanda ao **OK**.

### Fase E — Estoque
- Flag **estoque obrigatório** (config).
- **Entrada**; **saída automática** na venda; bloqueio + mensagem se obrigatório e sem saldo; permitir negativo se desligado.
- **Ajuste / zerar** saldo para reorganizar.

### Fase F — Dashboard e relatório
- **Receita** (vendido) como indicador principal; totais por forma de pagamento.
- Relatório por período + imprimir.

### Fase G — Polimento mobile-first e docs *(concluída)*

Checklist:

- **Viewport / PWA leve:** `lang="pt-BR"`, `viewport-fit=cover`, `theme-color` alinhado ao tema escuro. *(ok)*
- **Safe areas:** `env(safe-area-inset-*)` na topbar, conteúdo principal, drawer de mesa (mobile), login, pilha de alertas e modais críticos. *(ok)*
- **Toque:** botões `.btn` e ações compactas (`.sm` em alertas / pedido) com altura mínima ~44px; menu hambúrguer e itens do nav lateral confortáveis no celular. *(ok)*
- **Layout:** atalhos do início em coluna largura total em telas estreitas; tabela de pedidos com área de toque maior no link “Abrir”. *(ok)*
- **Telas operacionais (mobile):** Estoque, Relatório, Mesas, Pedido, lista de Pedidos — validação em celular. *(ok)*
- **Docs:** `frontend/PROJETO.md` e este plano mantidos alinhados ao comportamento real. *(ok)*

---

## 8.1 Pós-MVP (próximas decisões de produto)

Não fazem parte do ciclo A–G; escolher por prioridade de negócio:

- **Cadastros na aplicação:** usuários, colaboradores, cardápio (hoje há seed / dados iniciais).
- **Fiscal:** NFC-e/NF-e ou integração com emissor; hoje só referência em cupom.
- **Impressão:** bridge ESC/POS ou app auxiliar além do texto do comprovante.
- **Infra:** banco além do H2 em arquivo se precisar de múltiplos servidores ou backup operacional mais rígido; **CORS** restrito em ambiente exposto à internet.

---

## 9. Riscos e pontos de atenção (restantes)

- **Duas impressões:** comanda cozinha vs cupom cliente — manter templates distintos.
- **Performance** das agregações de relatório com H2 em arquivo (aceitável no MVP; otimizar depois se preciso).

---

## 10. Referência cruzada

- `contexto/REGRAS-PROJETO.md`
- `.cursor/rules/espetinho-jurema.mdc` e `.cursor/rules/espetinho-jurema-fases.mdc`

---

**Versão:** 1.4  
**Status:** **MVP fechado:** fases **A–G** concluídas (incl. validação mobile).  
**Próximo passo:** definir **uma** prioridade em **§8.1 Pós-MVP** (ex.: cadastros na UI, fiscal, impressão térmica ou troca de banco) e abrir escopo incremental.
