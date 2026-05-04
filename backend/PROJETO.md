# Documentação do backend — Espetinho Jurema API

## Visão geral

API REST em **Spring Boot 3.4** com **Java 21**, persistência **H2** (modo arquivo), **JPA/Hibernate** e notificações **STOMP** para alterações em pedidos. O desenho segue **arquitetura limpa** em um único módulo Maven: regras de negócio e portas na camada de aplicação, detalhes de framework na infraestrutura, adaptadores implementando portas de saída.

## Pacotes (`br.com.espetinhojurema`)

| Pacote | Papel |
|--------|--------|
| `domain.model` | Enums e objetos de domínio (`Mesa`, `MesaStatus`, `PedidoTipo`, `PedidoStatus`). |
| `domain.exception` | `BusinessException` para erros de regra de negócio (HTTP 400 na API). |
| `application.model` | Modelos de leitura retornados aos casos de uso (views/records). |
| `application.command` | Comandos imutáveis (`CriarPedidoCommand`). |
| `application.port.out` | Portas de persistência e eventos (`MesasPersistencePort`, `PedidosPersistencePort`, `PedidoEventPublisherPort`, etc.). |
| `application.service` | Orquestração (`MesaOperacoesService`, `ComprovanteTextoService`). |
| `infrastructure.persistence` | Entidades JPA, repositórios Spring Data, adaptadores que implementam as portas. |
| `infrastructure.messaging` | WebSocket/STOMP e publicação de eventos. |
| `infrastructure.backup` | Job agendado de backup H2. |
| `infrastructure.config` | CORS MVC, dados iniciais (`DataInitializer`). |
| `api` | Controllers REST, DTOs de entrada, `GlobalExceptionHandler`. |

## Princípios aplicados

- **SOLID**: serviços com dependência de abstrações (portas); adaptadores isolam JPA.
- **REST**: recursos versionados em `/api/v1`, uso de verbos HTTP e códigos de status coerentes.
- **Clean code**: records com DTOs imutáveis, nomes explícitos, transações nos adaptadores que alteram agregados.

## Principais fluxos

1. **Abrir mesa** (`MesaOperacoesService.abrirMesa`): valida mesa livre e ausência de pedido ativo; delega `PedidosPersistencePort.criarPedido` com `PedidoTipo.MESA`, que persiste pedido, associa colaborador/cliente opcional e marca mesa como `OCUPADA`.
2. **Itens e status**: `PedidosPersistenceAdapter` atualiza itens e status; ao marcar `PAGO` ou `CANCELADO`, libera a mesa se não houver outro pedido ativo na mesma mesa.
3. **Eventos**: após criar pedido, adicionar item ou mudar status, `StompPedidoEventPublisher` publica em `/topic/pedidos`.

## Endpoints REST (resumo)

| Método | Caminho | Descrição |
|--------|---------|-----------|
| GET | `/api/v1/mesas` | Lista mesas com indicação de pedido aberto. |
| GET | `/api/v1/mesas/resumo` | Totais para painel. |
| POST | `/api/v1/mesas/{id}/abrir` | Abre pedido na mesa. |
| PATCH | `/api/v1/mesas/{id}/status` | Atualiza status da mesa. |
| GET | `/api/v1/pedidos` | Lista com `status` e `tipo` opcionais. |
| GET | `/api/v1/pedidos/{id}` | Detalhe com itens. |
| POST | `/api/v1/pedidos/avulsos` | Pedido `BALCAO` ou `DELIVERY`. |
| POST | `/api/v1/pedidos/{id}/itens` | Adiciona item. |
| PATCH | `/api/v1/pedidos/{id}/status` | Altera status. |
| GET | `/api/v1/pedidos/{id}/comprovante` | Texto do cupom (`text/plain`). |
| GET | `/api/v1/categorias`, `/api/v1/produtos` | Catálogo. |
| GET/POST | `/api/v1/clientes` | Listagem e criação. |
| GET | `/api/v1/colaboradores` | Ativos. |
| GET/POST | `/api/v1/caixa/status`, `/abrir`, `/fechar` | Sessão de caixa. |

## Configuração

Arquivo `src/main/resources/application.yml`: JPA `ddl-auto: update` (adequado ao MVP; em produção estrita considerar migrações Flyway/Liquibase), CORS a partir de `app.cors.allowed-origins`, backup em `app.backup.*`.

## Segurança

Não há autenticação nesta versão; a API e o console H2 ficam expostos conforme configuração. Para ambiente público, adicionar Spring Security, desabilitar ou proteger `/h2-console` e restringir origens CORS.
