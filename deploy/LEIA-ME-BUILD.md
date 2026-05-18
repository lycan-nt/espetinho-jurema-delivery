# Como gerar o pacote para instalar no cliente

Execute **uma vez** (na sua máquina ou em CI) para gerar a pasta **`deploy/instalar`**, que você copia para o PC do cliente (Windows ou rodando o `.jar` no Mac/Linux).

**Importante:** os scripts `.bat`, `.xml` e o **LEIA-ME-INSTALACAO.md** ficam versionados na pasta `deploy/instalar`. O arquivo **`app/espetinho-app.jar`** só aparece **depois** de rodar o build abaixo.

## Pré-requisitos

- **Node.js** (LTS) — para build do Angular  
- **npm**  
- **Java 21** — conforme `backend/pom.xml`  
- **Maven** — opcional se existir **`backend/mvnw`** (Maven Wrapper); o **`deploy/build-pacote`** usa `./mvnw` antes de `mvn`.

## Passo a passo

### Mac ou Linux

Na raiz do repositório (`frontend`, `backend`, `deploy`):

```bash
chmod +x deploy/build-pacote.sh
./deploy/build-pacote.sh
```

### Windows

Prompt de comando na raiz do repositório:

```bat
deploy\build-pacote.bat
```

O script:

1. Instala dependências do frontend se precisar (`npm install`)
2. Build de produção do Angular
3. Copia o frontend para `backend/src/main/resources/static`
4. Gera o JAR com Maven (`package -DskipTests`)
5. Copia o JAR para **`deploy/instalar/app/espetinho-app.jar`** e os scripts de backup em **`deploy/instalar/app/`**

Opcionalmente gera **`deploy/instalar-8081`** com **`rodar-8081.sh`** (mesmo JAR, porta **8081**) para testes em Mac/Linux.

## Pacote final

Copie a pasta **`deploy/instalar`** inteira para o cliente e siga **`deploy/instalar/LEIA-ME-INSTALACAO.md`**.

```
deploy/
  build-pacote.bat / build-pacote.sh
  LEIA-ME-BUILD.md
  restaurar-backup.bat       → copiado para instalar\ pelo build-pacote.bat
  COMO-RESTAURAR-BACKUP.txt  → copiado para instalar\
  instalar/
    app/
      espetinho-app.jar      ← gerado pelo build
      data/
        LEIA-ME.txt          ← opcional; criado pelo build-pacote.bat
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

No cliente Windows é preciso baixar o **WinSW-x64.exe** para instalar como serviço — ver o LEIA-ME de instalação.

## Nome do JAR gerado pelo Maven

O build espera **`backend/target/espetinho-jurema-api-2.1.1-SNAPSHOT.jar`**. Se mudar `version` ou `artifactId` no `pom.xml`, atualize também **`deploy/build-pacote.sh`** e **`deploy/build-pacote.bat`** (variável `JAR_NAME`).
