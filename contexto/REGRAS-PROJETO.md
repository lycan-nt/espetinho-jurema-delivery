# Regras e diretrizes — Espetinho Jurema Delivery

Documento de referência para continuidade da implementação. Complementa `README.md` e as documentações em `backend/PROJETO.md` e `frontend/PROJETO.md`.

## 1. Referência de produto (sistema legado)

- Na pasta **`contexto/`** estão **capturas de tela** do sistema que o cliente já utiliza. Elas definem **referência visual e funcional** (fluxos, densidade de informação, hierarquia de ações), **não** cópia pixel-perfect obrigatória.
- Ao implementar novas telas ou fluxos, **consulte essas imagens** para manter coerência com a operação real (mesas, pedidos, delivery, caixa, etc.).
- Quando houver conflito entre “igual ao legado” e **usabilidade em celular** ou **acessibilidade**, prevalecem as regras da seção 3 (mobile-first e UX), documentando a decisão em PR/commit quando relevante.

## 2. Stack e entrega

| Camada | Tecnologia | Observação |
|--------|------------|------------|
| Backend | Java 21, Spring Boot | API REST, persistência, integrações |
| Frontend | Angular (versão alinhada ao projeto) | **SPA no navegador** — sem exigência de app nativo no escopo atual |
| Banco | H2 (arquivo) + rotina de backup | Configurável por ambiente |

- O sistema deve funcionar **no computador e no celular pelo mesmo navegador** (URL única), responsivo e tocável.
- **Não** assumir teclado ou tela grande como obrigatórios para concluir tarefas críticas do dia a dia.

## 3. Mobile-first e UX (frontend)

- **Mobile-first:** projetar primeiro para viewport estreito; depois expandir para tablet/desktop. Breakpoints e componentes devem evitar “adaptação forçada” só no final.
- **Toque:** áreas clicáveis adequadas (mínimo ~44×44 px lógicos), espaçamento entre ações destrutivas e primárias.
- **Feedback:** estados de carregamento, erro e sucesso visíveis; evitar tela “morta” sem indicação.
- **Navegação:** em telas pequenas, preferir **drawer, bottom sheet ou página dedicada** em vez de painéis laterais largos que não cabem.
- **Identidade:** manter coerência com a marca (cores, tipografia, logo em `frontend/public/assets/`) e contraste legível (incluindo modo escuro já adotado).
- **Performance:** lazy loading de rotas quando fizer sentido; evitar trabalho pesado na thread principal sem necessidade.

## 4. Arquitetura limpa e hexagonal (backend)

- **Domínio** sem dependência de frameworks: entidades/regras puras, enums, exceções de negócio.
- **Casos de uso / aplicação:** orquestram regras; dependem de **portas** (interfaces), não de implementações concretas.
- **Adaptadores de saída:** JPA, filas, e-mail, etc. implementam as portas; **adaptadores de entrada:** controllers REST, eventualmente mensageria.
- **Direção das dependências:** de fora para dentro — infraestrutura e API dependem da aplicação/domínio, não o contrário.
- **Transações:** limitar a camada que persiste agregados; evitar “god service” sem fronteiras claras.

## 5. Clean Code e SOLID (ambos os lados)

- **Nomes** expressivos; funções pequenas e com um propósito claro.
- **DRY** com critério: não abstrair antes de haver segunda ocorrência real.
- **S** — responsabilidade única por classe/módulo.  
- **O** — extensível via novos tipos/adaptadores, sem editar núcleo o tempo todo.  
- **L** — substituibilidade de implementações de portas sem quebrar contratos.  
- **I** — interfaces de porta pequenas e focadas.  
- **D** — depender de abstrações (portas) na aplicação.

## 6. API REST

- Recursos nomeáveis, verbos HTTP corretos, códigos de status consistentes.
- Versão da API em caminho (`/api/v1/...`) quando aplicável.
- Contratos estáveis: DTOs de entrada/saída explícitos; erros de negócio com mensagens claras para o front.

## 7. Contexto de tempo real e impressão

- Eventos em tempo real (ex.: WebSocket/STOMP) como **notificação**; o front deve poder **sincronizar** mesmo com falha pontual da conexão (retry, refetch).
- Comprovante / impressão: texto ou integração documentada; **não** misturar regra fiscal no core de domínio sem porta explícita.

## 8. Docker e ambientes

- Manter `Dockerfile` e `docker-compose` alinhados à documentação do `README.md`.
- Variáveis de ambiente para caminhos de banco e backup; **não** commitar segredos.

## 9. Evolução deste documento

- Novos requisitos (pagamento, NF-e, multi-loja, etc.) devem ser acrescentados aqui **antes** ou **junto** com a implementação correspondente.
- Imagens novas podem ser adicionadas em `contexto/` com nomes descritivos quando possível (facilita referência em PRs).

---

**Última atualização:** criado para alinhar implementação contínua com referência visual em `contexto/` e boas práticas acordadas.
