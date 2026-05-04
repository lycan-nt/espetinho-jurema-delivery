# Espetinho Jurema — Instalação no cliente (Windows)

Copie a pasta **`instalar`** (gerada pelo build — ver **`deploy/LEIA-ME-BUILD.md`**) para o PC do cliente (ex.: `C:\EspetinhoJurema`).

## Requisitos

- **Windows** 10 ou 11.
- **Java 21** instalado e no PATH.  
  Sugestão: [Adoptium Temurin 21](https://adoptium.net/temurin/releases/?version=21&os=windows).  
  Confira no Prompt: `java -version`.

## Estrutura recomendada

```
instalar/
  app/
    espetinho-app.jar       ← gerado pelo script build-pacote (antes de copiar para o cliente)
  config/                   ← opcional
  data/                     ← H2 e backups (criado ao rodar)
  logs/
  restaurar-backup.bat
  COMO-RESTAURAR-BACKUP.txt
  start.bat
  start-servico.bat
  install-servico.bat
  uninstall-servico.bat
  iniciar-com-windows-atalho.bat
  espetinho-jurema-servico.xml
  LEIA-ME-INSTALACAO.md
```

**Importante:** `start.bat` roda na pasta **`instalar`** e usa `app\espetinho-app.jar`. Os dados do H2 ficam em **`instalar\data`** (relativo ao diretório de trabalho ao iniciar o Java).

## Opção 1 — Manual (sem serviço)

1. Copie a pasta **instalar** completa para o cliente.
2. Garanta que existe **`app\espetinho-app.jar`** (rodando **`deploy\build-pacote.bat`** antes de copiar).
3. Dois cliques em **start.bat**.
4. Navegador: **http://localhost:9090** ou **http://IP-DESTE-PC:9090** na rede.

## Opção 2 — Atalho ao logar

Execute **iniciar-com-windows-atalho.bat** (cria atalho na pasta Iniciar do usuário).

## Opção 3 — Serviço Windows (sobe ao ligar o PC)

1. Baixe **WinSW-x64.exe** em [releases WinSW](https://github.com/winsw/winsw/releases) e coloque na mesma pasta que **install-servico.bat**.
2. Botão direito em **install-servico.bat** → **Executar como administrador**.
3. Para iniciar agora (Prompt como administrador): `net start EspetinhoJurema`.
4. Para remover: **uninstall-servico.bat** como administrador.

O nome do serviço é **EspetinhoJurema** (confira em `services.msc`).

## Configuração opcional (`config/application.properties`)

Para sobrescrever só no cliente (sem rebuild):

1. Crie **`instalar\config\application.properties`**.
2. Exemplo de linhas úteis:
   - `server.port=9091` — se a porta **9090** estiver ocupada.
   - Variáveis já documentadas no `application.yml` do projeto.

Os scripts **start.bat** e **start-servico.bat** carregam automaticamente `config\` se existir.

## Backup e restauração

- Backups automáticos: `data\backups\espetinho-backup-*.zip` (conforme agendamento).
- Restaurar: **`COMO-RESTAURAR-BACKUP.txt`** e **`restaurar-backup.bat`** na **mesma pasta do `start.bat`** (raiz da pasta **instalar**).

## Problemas comuns

| Sintoma | O que fazer |
|---------|--------------|
| `java` não reconhecido | Instalar Java 21 e abrir novo Prompt. |
| Porta em uso | `server.port=9091` (ou outra livre) em `config/application.properties`. |
| Jar ausente | Rodar **`deploy\build-pacote.bat`** e copiar de novo a pasta **`instalar`**. |
