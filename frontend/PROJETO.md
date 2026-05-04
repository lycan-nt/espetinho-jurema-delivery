# Documentação do frontend — Espetinho Jurema Web

## Visão geral

Aplicação **Angular 19** (standalone components, lazy routes, `inject()`, HttpClient com `fetch`), focada em **uso em desktop e celular**: navegação lateral recolhível, grade de mesas com filtros, painel de detalhes em drawer (em mobile comporta-se como folha inferior em altura limitada), tema **escuro** com acento **laranja** (`#e63900`) alinhado à identidade da marca.

## Estrutura

| Caminho | Função |
|---------|--------|
| `src/app/layout/shell.component.*` | Layout global: marca, menu (Início, Mesas, Pedidos; Estoque e Relatório para perfil atendimento), relógio, indicador de caixa, painel de alertas (WS). |
| `src/app/pages/inicio/` | Cartões de resumo e atalhos. |
| `src/app/pages/mesas/` | Controle de mesas + formulário “Iniciar”. |
| `src/app/pages/delivery/`, `balcao/` | Código legado / fora das rotas atuais do MVP (mesas + pedidos). |
| `src/app/pages/pedidos/` | Tabela de pedidos com filtros. |
| `src/app/pages/pedido-detalhe/` | Itens, inclusão de produtos, alteração de status, links de impressão de cupom. |
| `src/app/core/api-backend.service.ts` | Cliente HTTP da API v1. |
| `src/app/core/realtime.service.ts` | SockJS + STOMP; `/topic/pedidos` e `/topic/atendimento/alertas`. |
| `src/app/models/api.models.ts` | Tipos alinhados aos records JSON do backend. |
| `src/environments/` | `environment.ts` (dev aponta para `localhost:9090`); produção usa origem atual e `/api/v1`. |

## UI / UX

- Tipografia do sistema, contraste alto, botões e alvos de toque amplos nos fluxos críticos.
- Logo em `public/assets/logo.png` (referência visual do cliente).
- Listas e pedidos reagem a eventos WebSocket para aproximar o fluxo “celular lança → cozinha/desktop vê”.

## Mobile-first (Fase G)

- **`index.html`:** `lang="pt-BR"`, `viewport-fit=cover`, `theme-color` para barra do sistema.
- **Safe areas:** padding com `env(safe-area-inset-*)` no shell (topbar + área de conteúdo), menu lateral em breakpoint mobile, pilha de alertas de atendimento, login, drawer inferior de mesa (`styles.scss` / `shell.component.scss` / `alertas-atendimento-panel.component.scss`).
- **Toque:** `.btn` com altura mínima ~44px; botões `.sm` em alertas e pedido alinhados ao mesmo critério; link “Abrir” na lista de pedidos com área ampliada em telas estreitas.
- **Layout:** em telas ≤520px, atalhos da página Início passam a coluna com botões largura total (`inicio.component.scss`).
- **Estoque / relatório:** tabelas com `overflow-x` + toque inercial; formulários e filtros em coluna em telas estreitas; grade de mesas ligeiramente mais compacta em telas muito estreitas (`estoque`, `relatorio-faturamento`, `styles.scss` global para `.grid-mesax`).
- **Pedido (detalhe):** toolbar de status e cupons em coluna largura total ≤640px (`pedido-detalhe.component.scss`).

## Build e deploy

- Desenvolvimento: `npm start` (proxy não obrigatório; CORS liberado no backend para `:4200`).
- Produção: `ng build` substitui `environment.ts` por `environment.prod.ts` (via `angular.json`), gerando artefatos em `dist/frontend/browser` para servir atrás do Nginx definido no `Dockerfile`.

## Observações

- `@stomp/stompjs` e `sockjs-client` são CommonJS; o build emite aviso de otimização, aceitável para o escopo atual.
- Impressão térmica no browser: abre-se o URL do comprovante em nova aba; o operador pode usar “Imprimir” ou integrar depois com bridge nativo/ESC-POS.
