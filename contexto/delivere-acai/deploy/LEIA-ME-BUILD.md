# Como gerar o pacote para as lojas

Execute **uma vez** (na sua máquina ou em CI) para gerar a pasta que será copiada para cada um dos 3 PCs.

**Importante:** a pasta `instalar` já existe com os scripts e a documentação, mas o arquivo **`app/acai-app.jar`** só é gerado quando você roda o build abaixo. Até lá, não há pacote completo para copiar para as lojas.

## Pré-requisitos

- **Node.js** (LTS) – para build do frontend  
- **npm** – vem com o Node  
- **Java 17** – para build do backend  
- **Maven** (ou use o `mvnw` do backend) – para empacotar o JAR  

## Passo a passo

### No Mac (ou Linux)

1. Abra o **Terminal** na raiz do projeto (pasta onde estão `frontend`, `backend` e `deploy`).

2. Execute:
   ```bash
   ./deploy/build-pacote.sh
   ```
   (Se der “permission denied”, antes rode: `chmod +x deploy/build-pacote.sh`)

### No Windows

1. Abra o **Prompt de comando** na raiz do projeto.

2. Execute:
   ```bat
   deploy\build-pacote.bat
   ```

3. O script vai:
   - instalar dependências do frontend (se precisar)
   - fazer o build de produção do frontend (Angular)
   - copiar o frontend para o backend (`static`)
   - gerar o JAR do backend (com o frontend dentro)
   - copiar o JAR para **deploy\instalar\app\acai-app.jar**

4. A pasta **deploy\instalar** é o **pacote final**.  
   Copie a pasta **instalar** inteira para cada um dos 3 computadores das lojas e siga o **LEIA-ME-INSTALACAO.md** que está dentro dela.

## Estrutura após o build

```
deploy/
  build-pacote.bat    <- script que você rodou
  LEIA-ME-BUILD.md    <- este arquivo
  instalar/           <- copiar isso para cada loja
    app/
      acai-app.jar
    start.bat
    start-servico.bat
    install-servico.bat
    uninstall-servico.bat
    acai-servico.xml
    LEIA-ME-INSTALACAO.md
    (WinSW-x64.exe – baixar em cada PC; ver LEIA-ME-INSTALACAO)
```

## Se não tiver Maven instalado

O script tenta usar **mvnw.cmd** (Maven Wrapper) se existir na pasta **backend**. Se não existir, usa o **mvn** do sistema.  
Para gerar o wrapper no backend (uma vez):

```bat
cd backend
mvn -N io.qameta.allure:allure-maven:wrapper
```

Ou instale o Maven: https://maven.apache.org/download.cgi
