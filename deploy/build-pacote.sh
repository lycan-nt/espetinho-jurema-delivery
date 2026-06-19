#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND="$ROOT/frontend"
BACKEND="$ROOT/backend"
STATIC="$BACKEND/src/main/resources/static"
INSTALAR="$ROOT/deploy/instalar"
JAR_NAME="espetinho-jurema-api-2.2.1-SNAPSHOT.jar"
DEPLOY_JAR="espetinho-app.jar"

echo "========================================"
echo " Build do pacote Espetinho Jurema (Mac/Linux)"
echo "========================================"
echo

echo "[1/5] Build do frontend (Angular)..."
cd "$FRONTEND"
AJV_MARKER="node_modules/ajv/dist/vocabularies/applicator/index.js"
if [ ! -d node_modules ] || [ ! -f "$AJV_MARKER" ]; then
  if [ -d node_modules ] && [ ! -f "$AJV_MARKER" ]; then
    echo "node_modules incompleto (ex.: falta ajv). Removendo e reinstalando..."
    rm -rf node_modules
  fi
  echo "Instalando dependências do frontend..."
  if [ -f package-lock.json ]; then
    npm ci
  else
    npm install
  fi
fi
npm run build -- --configuration=production
echo "OK."
echo

echo "[2/5] Copiando frontend para backend (static)..."
rm -rf "$STATIC"
mkdir -p "$STATIC"
if [ -d "$FRONTEND/dist/frontend/browser" ]; then
  cp -R "$FRONTEND/dist/frontend/browser/"* "$STATIC/"
elif [ -d "$FRONTEND/dist/frontend" ]; then
  cp -R "$FRONTEND/dist/frontend/"* "$STATIC/"
else
  echo "ERRO: pasta dist/frontend não encontrada após o build."
  exit 1
fi
echo "OK."
echo

check_java_21() {
  local java_bin=""
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    java_bin="$JAVA_HOME/bin/java"
  elif command -v java >/dev/null 2>&1; then
    java_bin="java"
  fi
  if [ -z "$java_bin" ]; then
    echo "ERRO: Java não encontrado. Instale JDK 21 e defina JAVA_HOME."
    echo "  Windows (winget): winget install Microsoft.OpenJDK.21"
    echo "  Depois (Git Bash): export JAVA_HOME=\"/c/Program Files/Microsoft/jdk-21.0.8.9-hotspot\""
    exit 1
  fi
  local major
  major=$("$java_bin" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
  if [ -z "$major" ] || [ "$major" -lt 21 ]; then
    echo "ERRO: o backend exige JDK 21+, mas foi detectado Java ${major:-?}."
    echo "  JAVA_HOME atual: ${JAVA_HOME:-(não definido)}"
    echo "  Versão: $("$java_bin" -version 2>&1 | head -1)"
    echo
    echo "Instale JDK 21, aponte JAVA_HOME para ele e rode o build de novo."
    echo "  winget install Microsoft.OpenJDK.21"
    exit 1
  fi
  echo "Java OK: $("$java_bin" -version 2>&1 | head -1)"
}

echo "[3/5] Build do backend (Maven)..."
check_java_21
cd "$BACKEND"
if [ -x ./mvnw ]; then
  ./mvnw -q package -DskipTests
else
  mvn -q package -DskipTests
fi
if [ ! -f "$BACKEND/target/$JAR_NAME" ]; then
  echo "ERRO: JAR não gerado em target/$JAR_NAME"
  exit 1
fi
echo "OK."
echo

echo "[4/5] Montando pasta deploy/instalar..."
rm -rf "$INSTALAR/app"
mkdir -p "$INSTALAR/app/data"
cp "$BACKEND/target/$JAR_NAME" "$INSTALAR/app/$DEPLOY_JAR"
if [ -f "$ROOT/deploy/restaurar-backup.bat" ]; then
  cp "$ROOT/deploy/restaurar-backup.bat" "$INSTALAR/restaurar-backup.bat"
fi
if [ -f "$ROOT/deploy/COMO-RESTAURAR-BACKUP.txt" ]; then
  cp "$ROOT/deploy/COMO-RESTAURAR-BACKUP.txt" "$INSTALAR/COMO-RESTAURAR-BACKUP.txt"
fi
echo "OK."
echo

echo "[5/5] Pasta opcional deploy/instalar-8081 (porta 8081)..."
INSTALAR81="$ROOT/deploy/instalar-8081"
rm -rf "$INSTALAR81"
mkdir -p "$INSTALAR81/app/data"
cp "$BACKEND/target/$JAR_NAME" "$INSTALAR81/app/$DEPLOY_JAR"
cat > "$INSTALAR81/rodar-8081.sh" << 'EOS'
#!/usr/bin/env bash
cd "$(dirname "$0")/app"
java -jar espetinho-app.jar --server.port=8081
EOS
chmod +x "$INSTALAR81/rodar-8081.sh"

echo
echo "========================================"
echo " Pacotes prontos:"
echo " - deploy/instalar       (porta 9090)"
echo " - deploy/instalar-8081  (./rodar-8081.sh — porta 8081)"
echo "========================================"
