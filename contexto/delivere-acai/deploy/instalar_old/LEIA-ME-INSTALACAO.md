# Delivere Açaí – Instalação em cada loja (Windows)

Use esta pasta **em cada um dos 3 computadores** (uma cópia por loja). Cada máquina terá seu próprio servidor e seus próprios dados.

## Requisitos

- **Windows** 10 ou 11 (ou Server).
- **Java 17** instalado e no PATH.  
  - Download: [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17&os=windows) (escolha o instalador .msi).
  - Depois de instalar, abra um novo “Prompt de comando” e digite `java -version` para confirmar.

## Estrutura da pasta (em cada PC)

```
instalar/
  app/
    acai-app.jar      <- aplicação (backend + frontend)
  config/             <- (opcional) configuração por loja
    application.properties
  data/               <- criado ao rodar; banco H2 fica aqui
  logs/               <- criado ao rodar; logs do serviço
  start.bat                    <- iniciar manualmente
  start-servico.bat            <- usado pelo serviço Windows
  iniciar-com-windows-atalho.bat <- atalho para abrir ao logar (Opção 2a)
  install-servico.bat          <- instalar serviço para abrir com o Windows (Opção 2b)
  uninstall-servico.bat
  acai-servico.xml    <- configuração do serviço
  WinSW-x64.exe       <- baixar (veja abaixo)
```

## Opção 1 – Só abrir quando quiser (sem serviço)

1. Copie a pasta **instalar** para o PC (ex.: `C:\DelivereAcai`).
2. Dê dois cliques em **start.bat**.
3. Abra o navegador em: **http://localhost:8080**  
   (ou **http://IP-DESTA-MAQUINA:8080** em outros PCs da rede).
4. Para fechar, feche a janela do **start.bat**.

## Opção 2a – Abrir quando o usuário fizer login (sem ser administrador)

1. Dê dois cliques em **iniciar-com-windows-atalho.bat**.
2. Será criado um atalho em “Iniciar com o Windows”. O servidor abrirá quando alguém fizer login neste PC.
3. Para remover, apague o atalho em:  
   `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup`.

## Opção 2b – Abrir junto com o Windows (serviço – recomendado)

Assim o sistema sobe sozinho quando o computador ligar (não precisa estar logado).

1. **Baixar o WinSW**  
   - https://github.com/winsw/winsw/releases  
   - Baixe **WinSW-x64.exe** (64 bits) ou **WinSW.exe** (32 bits).  
   - Coloque o `.exe` **dentro da pasta instalar** (no mesmo nível de `start.bat`).

2. **Instalar o serviço**  
   - Clique com o botão direito em **install-servico.bat**.  
   - Escolha **“Executar como administrador”**.  
   - O serviço **“Delivere Acai”** será instalado e configurado para iniciar com o Windows.

3. **Iniciar agora (opcional)**  
   - Abra o Prompt de comando **como administrador** e digite:  
     `net start DelivereAcai`

4. Acesse: **http://localhost:8080** (ou pelo IP da máquina na rede).

**Dica:** Se o install-servico.bat abrir e fechar na hora, é porque não foi executado como administrador. Use sempre botão direito → Executar como administrador. Para conferir o serviço: Win+R → `services.msc` → procure "Delivere Acai".

Para **parar** o serviço: `net stop DelivereAcai`  
Para **remover** o serviço: execute **uninstall-servico.bat** como administrador.

## Configuração por loja (opcional)

Se cada loja precisar de algo diferente (usuários de gestão, planilha Google, etc.):

1. Crie a pasta **config** dentro de **instalar** (se ainda não existir).
2. Copie o `application.properties` do projeto (ou do backend) para **config/application.properties**.
3. Ajuste só o que for da loja (por exemplo):
   - `app.gestao.usuarios=usuario1,usuario2`
   - `app.google.sheets.spreadsheet-id-planilha-diaria=...` (ID da planilha desta loja)
4. Reinicie o servidor (ou o serviço) para aplicar.

O que não estiver em **config/application.properties** continua vindo do que está dentro do JAR (valores padrão).

## Máquina com pouca memória

Se o PC for fraco, edite **start.bat** (e **start-servico.bat** se usar serviço) e use menos memória, por exemplo:

- Troque `-Xmx512m` por `-Xmx256m`.
- Se quiser, descomente a linha com `-Dspring.jmx.enabled=false` para reduzir um pouco o uso.

## Resumo por loja

| O quê              | Onde / Como |
|--------------------|-------------|
| Uma cópia da pasta | Uma pasta “instalar” por loja (ex.: C:\DelivereAcai). |
| Dados (banco)      | Cada pasta tem seu próprio **data/**; não misture entre lojas. |
| Iniciar com o PC   | Usar **install-servico.bat** (com WinSW) na pasta da loja. |
| Acesso             | http://localhost:8080 nessa máquina ou http://IP:8080 pela rede. |

## Problemas comuns

- **“java” não é reconhecido**  
  Instale o Java 17 e reinicie o computador (ou abra um novo Prompt de comando).

- **Porta 8080 em uso**  
  No **config/application.properties** (ou no properties do projeto) coloque, por exemplo:  
  `server.port=8081` e use a nova porta no navegador.

- **Serviço não inicia**  
  Rode **install-servico.bat** como administrador. Confirme que o **WinSW-x64.exe** está na pasta **instalar**. Veja os arquivos em **logs/** para mensagens de erro.
