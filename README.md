# Espetinho Jurema — Operação e delivery

Sistema para operação de salão (mesas, balcão, delivery) inspirado no fluxo de restaurante: lançamento pelo celular ou computador, lista de pedidos atualizada em tempo real e emissão de texto para cupom (incluindo referência a documento fiscal ou não fiscal). Stack: **Java 21**, **Spring Boot 3.4**, **Angular 19**, **H2** (arquivo em disco com backup agendado).

## Funcionalidades principais

- **Mesas**: grade com filtros (todas / ocupadas / encerrando / livres), painel de detalhes (responsável, cliente, pessoas, observações), ação **Iniciar** abre pedido vinculado à mesa.
- **Delivery e balcão**: criação de pedidos avulsos (`DELIVERY`, `BALCAO`).
- **Pedidos**: listagem com filtros, tela de detalhe com itens, mudança de status (preparo, pronto, pago, cancelado) e adição de itens.
- **Catálogo**: categorias e produtos (seed inicial na primeira execução).
- **Caixa**: status aberto/fechado (sessão inicial criada apenas quando não há histórico de caixa no banco).
- **Tempo real**: STOMP sobre WebSocket/SockJS no tópico `/topic/pedidos` após alterações em pedidos.
- **Cupom para impressora**: `GET /api/v1/pedidos/{id}/comprovante` retorna texto UTF-8 adequado para copiar ou enviar a um serviço de impressão térmica (integração ESC/POS no navegador costuma exigir app auxiliar ou impressão do texto gerado).

## Estrutura do repositório

| Pasta       | Conteúdo |
|------------|----------|
| `backend/` | API Spring Boot, arquitetura em camadas (ver `backend/PROJETO.md`). |
| `frontend/`| SPA Angular com layout escuro e identidade Espetinho Jurema (ver `frontend/PROJETO.md`). |

## Pré-requisitos

- **Backend**: JDK 21+, Maven 3.9+ (ou build apenas via Docker).
- **Frontend**: Node.js 20+, npm.
- **Docker** (opcional): Docker Compose v2.

## Como executar em desenvolvimento

### Backend

```bash
cd backend
mvn spring-boot:run
```

- API: `http://localhost:9090`
- Console H2 (desenvolvimento): `http://localhost:9090/h2-console` (JDBC URL conforme `application.yml`).
- Dados padrão: 40 mesas, colaborador **JODARIO**, cardápio de exemplo, caixa aberto na **primeira** carga quando não existe sessão.

Variáveis úteis:

| Variável       | Significado |
|----------------|-------------|
| `H2_FILE_PATH` | Caminho do arquivo H2 (ex.: `/app/data/espetinho` no Docker). |
| `H2_BACKUP_DIR`| Pasta de arquivos `.zip` gerados pelo comando `BACKUP` do H2. |

### Frontend

```bash
cd frontend
npm install
npm start
```

Abra `http://localhost:4200`. Em desenvolvimento, a API é resolvida pelo host da página (`localhost` no PC ou o mesmo IP da rede quando você abre pelo celular); veja **Acesso pelo celular (rede local)** abaixo.

### Acesso pelo celular (rede local)

1. **Mesma rede Wi‑Fi** que o computador que roda backend e frontend.
2. **IP do computador** (ex.: macOS: `ipconfig getifaddr en0`; Linux: `hostname -I`).
3. **Backend** em `0.0.0.0:9090` — o Spring Boot já escuta em todas as interfaces por padrão.
4. **CORS:** por padrão a API aceita qualquer origem (`*` em `application.yml`) para facilitar o celular na LAN. Reinicie o backend após atualizar o projeto.
5. **Frontend** escutando em todas as interfaces (`npm start` já usa `host: 0.0.0.0` no `angular.json`).
6. No celular, abra **`http://192.168.x.x:4200`** (o mesmo IP do passo 2).
7. Se a página abrir mas o login falhar com “sem resposta da API”, verifique **firewall** do Mac (permitir **Java** ou a porta **9090** na rede privada) e se o backend está rodando.

### Docker Compose (produção local)

Na raiz do projeto:

```bash
docker compose up --build
```

- Interface: `http://localhost` (Nginx servindo o build Angular e fazendo proxy de `/api` e `/ws` para o backend).
- API direta (opcional): `http://localhost:9090`.

Volume nomeado `espetinho-h2-data` mantém o arquivo do H2 e os backups entre reinícios.

## Backup do H2

O agendador `H2BackupScheduler` executa `BACKUP TO '<arquivo>.zip'` conforme `app.backup.cron` (padrão: 03:00 diariamente) e remove arquivos mais antigos que `app.backup.retention-days`. Em desenvolvimento os backups ficam em `./data/backups`; no container, em `H2_BACKUP_DIR`.

Para restaurar em atualização, pare a aplicação, substitua ou restaure o arquivo de banco a partir do ZIP oficial do H2 ou replique o procedimento de restore documentado pelo H2 a partir do backup gerado.

## Documentação interna

- [backend/PROJETO.md](backend/PROJETO.md) — camadas, portas, REST, WebSocket, persistência.
- [frontend/PROJETO.md](frontend/PROJETO.md) — rotas, serviços, tema e responsividade.

## Próximos passos sugeridos (fora do escopo atual)

- Autenticação/autorização (JWT ou sessão) e perfis (cozinha, caixa, garçom).
- Integração fiscal real (NFC-e/NF-e) e drivers de impressora.
- Relatórios e cadastros completos (clientes, produtos com estoque).

## Licença e uso

Projeto entregue para operação do cliente **Espetinho Jurema**; ajuste licenciamento conforme contrato.
