# Envio do relatório para Google Sheets

O relatório de vendas (comandas fechadas, total e totais por forma de pagamento) pode ser enviado para uma planilha no Google Drive usando a **Google Sheets API** com autenticação por **Service Account**.

## O que você precisa configurar

### 1. Google Cloud Console

1. Acesse [Google Cloud Console](https://console.cloud.google.com/) e crie um projeto (ou use um existente).
2. Ative a **Google Sheets API**: no menu lateral, **APIs e serviços** → **Biblioteca** → pesquise por “Google Sheets API” → **Ativar**.
3. Crie uma **conta de serviço**:
   - **APIs e serviços** → **Credenciais** → **Criar credenciais** → **Conta de serviço**.
   - Dê um nome (ex.: `acai-relatorio`) e conclua.
   - Na lista de contas de serviço, clique na que você criou → aba **Chaves** → **Adicionar chave** → **Criar nova chave** → **JSON**.
   - O arquivo JSON será baixado. Guarde em um local seguro (ex.: pasta do projeto, fora do controle de versão).

### 2. Google Sheets (planilha)

1. Crie uma planilha no [Google Sheets](https://sheets.google.com) (ou use uma existente).
2. Copie o **ID da planilha** na URL:
   - URL no formato: `https://docs.google.com/spreadsheets/d/<SPREADSHEET_ID>/edit`
   - O trecho `<SPREADSHEET_ID>` é o que você vai colar no `application.properties`.
3. **Compartilhe a planilha** com o e-mail da conta de serviço (ex.: `acai-relatorio@seu-projeto.iam.gserviceaccount.com`) com permissão **Editor**. Esse e-mail aparece no JSON baixado no campo `client_email`.

### 3. Backend (application.properties)

No `application.properties` (ou via variáveis de ambiente), defina:

```properties
# ID da planilha (da URL do Google Sheets)
app.google.sheets.spreadsheet-id=SEU_SPREADSHEET_ID_AQUI

# Caminho para o arquivo JSON da Service Account (absoluto ou relativo ao diretório de execução)
app.google.sheets.credentials-path=./config/google-service-account.json
```

Opcional:

```properties
# Nome da aba onde os dados serão escritos (padrão: Relatório)
app.google.sheets.sheet-name=Relatório
```

### Modo “Uma planilha por dia”

Se quiser **uma planilha nova por dia**, criada automaticamente e atualizada ao longo do dia:

```properties
app.google.sheets.uma-planilha-por-dia=true
# Título base das planilhas (a data é acrescentada, ex.: "Relatório Mix Açaí 2025-02-12")
app.google.sheets.titulo-planilha-dia=Relatório Mix Açaí
```

- Com `uma-planilha-por-dia=true`, o `spreadsheet-id` é ignorado.
- Na primeira vez que você envia no dia, o backend **cria** uma planilha nova no Google Drive da **conta de serviço** e guarda o ID no banco.
- Cada novo envio no mesmo dia **atualiza** essa mesma planilha (relatório completo daquele dia).
- As planilhas ficam na Drive da conta de serviço. Para ver no seu Drive, compartilhe a pasta da conta de serviço com seu e-mail ou abra o link da planilha (o ID fica gravado na tabela `planilha_dia` no banco).

**Importante:** Não versionar o JSON da conta de serviço. Adicione-o ao `.gitignore` (ex.: `config/google-service-account.json` ou `*-service-account.json`).

## Caixa e planilha do dia

Com **abertura/fechamento de caixa**:

- No **primeiro acesso do dia** (após login), o sistema exige **abertura de caixa**: informar o valor em caixa. Nesse momento é criada uma **nova aba** (com o nome da data, ex.: `2025-02-12`) em uma **planilha fixa** — não cria mais arquivos no Drive, evitando estourar a quota.
- A planilha usada é `app.google.sheets.spreadsheet-id-planilha-diaria` (se vazia, usa `app.google.sheets.spreadsheet-id`). **Compartilhe essa planilha** com o e-mail da conta de serviço (Editor).
- Um **job** envia o relatório do dia para a aba daquele dia **a cada X minutos** (`app.caixa.intervalo-envio-planilha-ms`).
- O botão **Fechar caixa** envia o relatório final para a aba do dia e fecha o caixa.
- O botão **Enviar para Google Sheets** na tela de Relatório envia o relatório **filtrado** para a planilha de relatórios manuais e **não altera** a aba diária do caixa.

Propriedades:

- `app.google.sheets.spreadsheet-id-planilha-diaria` — planilha onde são criadas as abas do caixa (uma por dia). Se vazio, usa `spreadsheet-id`.
- `app.caixa.intervalo-envio-planilha-ms=300000` — intervalo em ms (5 minutos).

## Uso

1. **Planilha do dia (caixa):** criada na abertura do caixa e atualizada automaticamente a cada X minutos e no fechamento.
2. **Relatório manual:** na tela Relatório, escolha o período e clique em **Enviar para Google Sheets**. O relatório vai para a planilha configurada em `spreadsheet-id` (não altera a planilha do dia).

## Resumo do que pegar/configurar

| Onde | O que fazer |
|------|----------------|
| **Google Cloud** | Criar projeto → Ativar Google Sheets API → Criar conta de serviço → Baixar JSON da chave |
| **Google Sheets** | Criar/abrir planilha → Copiar o **ID** da URL → Compartilhar a planilha com o **e-mail da service account** (Editor) |
| **Backend** | Preencher `app.google.sheets.spreadsheet-id` e `app.google.sheets.credentials-path` no `application.properties` |

Se `spreadsheet-id` ou `credentials-path` estiverem vazios, o botão “Enviar para Google Sheets” retornará erro informando que a integração não está configurada.
