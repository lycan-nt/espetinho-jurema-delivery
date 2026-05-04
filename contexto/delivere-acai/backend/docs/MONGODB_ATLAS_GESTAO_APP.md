# MongoDB Atlas – Sincronização para o app de gestão

O backend pode sincronizar os dados de gestão (totais do dia, comandas fechadas, caixas) para um banco **MongoDB Atlas** online. O app mobile consulta esses dados (via API `GET /api/gestao-app/resumo` que lê desse banco).

## 1. O que configurar no `application.properties`

No arquivo `backend/src/main/resources/application.properties` (ou via variáveis de ambiente):

| Propriedade | Obrigatório | Descrição |
|-------------|------------|-----------|
| `app.mongodb.uri` | **Sim** (para ativar) | URI de conexão do MongoDB Atlas. Se **não** estiver definida, o backend roda normalmente **sem** MongoDB (nenhuma sincronização). |
| `app.mongodb.database` | Não | Nome do banco no Atlas. Padrão: `acaigestao`. |
| `app.mongodb.sync-interval-ms` | Não | Intervalo em milissegundos para o job que atualiza o resumo no MongoDB. Padrão: `300000` (5 minutos). |

**Exemplo** (substitua usuário, senha e host pelo seu cluster):

```properties
app.mongodb.uri=mongodb+srv://SEU_USUARIO:SUA_SENHA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
app.mongodb.database=acaigestao
```

Ou via variável de ambiente (recomendado em produção):

```bash
export APP_MONGODB_URI="mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
```

**Importante:** Se `app.mongodb.uri` estiver vazia ou não existir, o backend **não** tenta conectar ao MongoDB. Não é preciso comentar nada: basta **não** definir a propriedade para rodar sem Atlas.

---

## 2. Como criar o banco no MongoDB Atlas

### 2.1 Criar conta e cluster

1. Acesse [https://www.mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas) e crie uma conta (ou faça login).
2. Crie um **Cluster** (ex.: M0 Free).
3. Em **Security → Database Access**, crie um usuário de banco (usuário e senha) com permissão **Atlas Admin** (ou **Read and write to any database**).
4. Em **Security → Network Access**, adicione o IP que vai acessar (ex.: **0.0.0.0/0** para qualquer IP, só para teste; em produção restrinja).

### 2.2 Obter a URI de conexão

1. No Atlas, clique em **Connect** no cluster.
2. Escolha **Drivers** (ou **Connect your application**).
3. Copie a **Connection String**, algo como:
   ```text
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
4. Substitua `<username>` e `<password>` pelo usuário e senha que você criou (caracteres especiais na senha devem ser codificados em URL, ex.: `@` → `%40`).

### 2.3 Criar o banco e a collection

Não é obrigatório criar nada antes: o **Spring Data MongoDB** cria o banco e a collection na primeira escrita.

- **Banco:** o nome é o definido em `app.mongodb.database` (padrão: `acaigestao`).
- **Collection:** o backend usa a collection `resumo_gestao` e grava um documento com `_id: "atual"` (um único documento atualizado a cada sync).

Se quiser criar manualmente no Atlas:

1. **Databases → Create Database**
   - Database name: `acaigestao` (ou o valor de `app.mongodb.database`).
   - Collection name: `resumo_gestao`.
2. Depois é só subir o backend com `app.mongodb.uri` definida; o primeiro sync vai preencher o documento.

---

## 3. Comportamento no backend

- **Ao fechar uma comanda:** o backend chama a sincronização para o MongoDB (atualiza o documento `resumo_gestao` com id `"atual"`).
- **A cada 5 minutos** (ou o valor de `app.mongodb.sync-interval-ms`): um job repete a sincronização com o relatório do dia.
- **API para o app:** `GET /api/gestao-app/resumo` (com JWT) retorna o documento de resumo (totais, comandas, caixas) lido do MongoDB. Se o MongoDB não estiver configurado, esse endpoint não existe (404).

---

## 4. Resumo do que você precisa fazer

1. **No `application.properties` (ou env):** definir `app.mongodb.uri` com a URI do Atlas quando quiser usar o app de gestão. Deixar **sem** essa propriedade para rodar sem MongoDB.
2. **No Atlas:** criar cluster, usuário, liberar IP e copiar a URI.
3. **Banco/collection:** podem ser criados automaticamente na primeira sincronização; se quiser, crie o banco `acaigestao` e a collection `resumo_gestao` manualmente.

Depois disso, o app mobile pode consumir os dados via `GET /api/gestao-app/resumo` (autenticado com o mesmo JWT do sistema).

---

## 5. App independente (login e gestão direto no MongoDB)

O app pode funcionar **sem servidor web**: acessa o MongoDB via **Atlas Data API**. Na subida do backend, os usuários (H2) são sincronizados para a collection **`app_usuarios`** (username, passwordHash bcrypt, setor). No Atlas, ative a **Data API** no App Services, crie uma API Key e vincule o cluster. No app, use a tela **Configurar MongoDB (Data API)** para informar URL base, API Key, Data Source e Database (`acaigestao`).
