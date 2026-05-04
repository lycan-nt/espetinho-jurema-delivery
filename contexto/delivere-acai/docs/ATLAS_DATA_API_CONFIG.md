# Ativar Data API e criar API Key no MongoDB Atlas

O app usa a **Data API** do Atlas para acessar o MongoDB (login e gestão) sem depender do backend. A Data API fica dentro do **App Services**, que nem sempre aparece no menu lateral — use os caminhos abaixo.

---

## Não encontro "Data API" nem "App Services" no menu

Se no menu lateral você só vê **Database**, **Streaming Data**, **Services**, **Security** e o cluster mostra **"LINKED APP SERVICES: None Linked"**, use um destes caminhos:

### 1) URL direta do App Services (recomendado)

1. Abra no navegador (já logado no Atlas): **https://services.cloud.mongodb.com**
2. Escolha a mesma organização e projeto (ex.: "Felipe's Org", "Project 0").
3. Você verá a área do App Services. Clique em **"Create a New App"** (ou "Create App") se ainda não tiver nenhum app.
4. Ao criar o app:
   - **Name:** ex. `Acaí Gestão`
   - **Linked Data Source:** selecione o cluster **AlfaTec** (anote o nome do data source, em geral `mongodb-atlas`).
   - Crie o app.
5. Dentro do app: **HTTPS Endpoints** → aba **Data API** → ative **Enable the Data API** → Save.
6. **Authentication** → **API Keys** → **Create API Key** → copie a chave (não será mostrada de novo).
7. A **URL base** usa o App ID que aparece na URL do navegador (`.../apps/XXXXXXXX-XXXX-.../`):  
   `https://data.mongodb-api.com/app/<APP_ID>/endpoint/data/v1`

### 2) Pelo cluster

No card do cluster **AlfaTec**, em **"LINKED APP SERVICES: None Linked"**, veja se há um link **"Link"** ou **"Link App"** e clique — deve abrir o App Services para criar/vincular um app. Depois siga os passos 4–7 acima.

### 3) Abas no topo

Acima da lista de clusters, veja se existe uma aba **"App Services"** ou **"Build"** e use-a para criar o app e ativar a Data API.

---

## Opção 1 – Data API direto no cluster (menu lateral do Atlas)

Em versões recentes do Atlas, a Data API pode aparecer **direto no menu lateral** da sua organização/deployment.

1. Acesse [https://cloud.mongodb.com](https://cloud.mongodb.com) e faça login.
2. No **menu lateral esquerdo**, procure por **"Data API"** (pode estar em **Build** ou na raiz do menu).
3. Clique em **Data API**.
4. Selecione o **cluster** que você usa (ex.: o que tem o banco `acaigestao`).
5. Clique em **"Enable the Data API"** (ou equivalente) para ativar.
6. **Criar API Key:**
   - Procure por **"Create API Key"** ou **"API Keys"** / **"Create Key"**.
   - Dê um nome (ex.: `App Gestão`).
   - Gere a chave e **copie e guarde** em um lugar seguro (ela não será mostrada de novo).
7. **URL base da Data API:**  
   Na mesma tela ou em **"Endpoint"** / **"Data API URL"**, deve aparecer algo como:
   ```text
   https://data.mongodb-api.com/app/<SEU_APP_ID>/endpoint/data/v1
   ```
   ou
   ```text
   https://<região>.data.mongodb-api.com/app/<SEU_APP_ID>/endpoint/data/v1
   ```
   **Anote essa URL** (substitua `<SEU_APP_ID>` pelo valor que aparecer).

O **Data Source** costuma ser o nome do cluster (ex.: `Cluster0` ou `mongodb-atlas`). Use o mesmo nome que aparece no Atlas.

---

## Opção 2 – Via App Services (Build)

Se **não** aparecer "Data API" no menu lateral, use o **App Services** (Build):

1. Acesse [https://cloud.mongodb.com](https://cloud.mongodb.com) e faça login.
2. No menu lateral, clique em **"Build"** (ou **"App Services"**).
3. **Criar um App** (se ainda não tiver):
   - Clique em **"Create a New App"**.
   - Nome (ex.: `Acaí Gestão`).
   - Escolha o **cluster** que tem o banco `acaigestao` e **link** o cluster como data source (o nome padrão costuma ser `mongodb-atlas` — anote).
   - Crie o app.
4. **Ativar a Data API:**
   - Dentro do app, no menu à esquerda, clique em **"HTTPS Endpoints"**.
   - Abra a aba **"Data API"**.
   - Ative a opção **"Enable the Data API"** (ou similar).
   - Escolha o **response type** (ex.: JSON).
   - **Save** / **Deploy**.
5. **Criar API Key:**
   - No menu do app, vá em **"Authentication"** (ou **"Access"**).
   - Aba **"API Keys"** (ou **"Create API Key"**).
   - Clique em **"Create API Key"**.
   - Nome (ex.: `App Gestão`).
   - Gere a chave e **copie e guarde** (não será mostrada de novo).
6. **URL base:**
   - A URL base da Data API usa o **App ID** do seu app.
   - Você vê o App ID na **URL do navegador** quando está no app, algo como:
     ```text
     https://services.cloud.mongodb.com/.../groups/.../apps/XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX/...
     ```
     O trecho `XXXXXXXX-XXXX-...` é o **App ID**.
   - A URL base da Data API fica:
     ```text
     https://data.mongodb-api.com/app/<APP_ID>/endpoint/data/v1
     ```
     Substitua `<APP_ID>` pelo App ID do seu app (com hífens).

---

## O que usar no app (Configurar MongoDB)

Na tela **"Configurar MongoDB (Data API)"** do app, preencha:

| Campo        | Exemplo / observação |
|-------------|----------------------|
| **URL base** | `https://data.mongodb-api.com/app/SEU_APP_ID/endpoint/data/v1` (ou a URL que o Atlas mostrar). Sem barra no final. |
| **API Key**  | A chave que você criou e copiou. |
| **Data Source** | Nome do data source do cluster (geralmente `mongodb-atlas` ou `Cluster0`). É o nome que aparece quando você vinculou o cluster ao app ou na tela da Data API. |
| **Database** | `acaigestao` (ou o nome do banco que o backend usa em `app.mongodb.database`). |

Salve e volte à tela de login para testar.

---

## Resumo

1. **Ativar a Data API** no Atlas (pelo menu "Data API" ou por App Services → HTTPS Endpoints → Data API).
2. **Criar uma API Key** e guardar o valor.
3. **Anotar a URL base** (com o App ID ou a que o Atlas exibir).
4. **Anotar o nome do Data Source** (cluster/data source linkado).
5. No app: **Configurar MongoDB (Data API)** → colar URL base, API Key, Data Source e Database (`acaigestao`).

Se a interface do Atlas estiver diferente do descrito, procure por **"Data API"** ou **"HTTPS Endpoints"** e **"API Keys"** no menu do projeto ou do app; os nomes podem variar levemente entre regiões e versões.
