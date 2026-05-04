#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND="$ROOT/frontend"
BACKEND="$ROOT/backend"
STATIC="$BACKEND/src/main/resources/static"
INSTALAR="$ROOT/deploy/instalar"

echo "========================================"
echo " Build do pacote Delivere Acai (Mac/Linux)"
echo "========================================"
echo

# 1) Build do frontend
echo "[1/5] Build do frontend (Angular)..."
cd "$FRONTEND"
if [ ! -d node_modules ]; then
  echo "Instalando dependências do frontend (npm install)..."
  npm install
fi
npm run build -- --configuration=production
echo "OK."
echo

# 2) Copiar saída do frontend para backend/static
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

# 3) Build do backend (JAR)
echo "[3/5] Build do backend (Maven)..."
cd "$BACKEND"
if [ -x ./mvnw ]; then
  ./mvnw -q package -DskipTests
else
  mvn -q package -DskipTests
fi
if [ ! -f "$BACKEND/target/acai-backend-1.0.0.jar" ]; then
  echo "ERRO: JAR não gerado."
  exit 1
fi
echo "OK."
echo

# 4) Copiar pacote para deploy/instalar
echo "[4/5] Montando pasta instalar..."
rm -rf "$INSTALAR/app"
mkdir -p "$INSTALAR/app"
cp "$BACKEND/target/acai-backend-1.0.0.jar" "$INSTALAR/app/acai-app.jar"
mkdir -p "$INSTALAR/app/data"

# 5) Pacote alternativo porta 8081
echo "[5/5] Montando pasta instalar-8081 (porta 8081)..."
INSTALAR81="$ROOT/deploy/instalar-8081"
rm -rf "$INSTALAR81"
mkdir -p "$INSTALAR81/app/data"
cp "$BACKEND/target/acai-backend-1.0.0.jar" "$INSTALAR81/app/acai-app.jar"
echo '#!/usr/bin/env bash
cd "$(dirname "$0")/app"
java -jar acai-app.jar --server.port=8081
' > "$INSTALAR81/rodar-8081.sh"
chmod +x "$INSTALAR81/rodar-8081.sh"
echo
echo "========================================"
echo " Pacotes prontos:"
echo " - deploy/instalar      (porta 8080)"
echo " - deploy/instalar-8081 (./rodar-8081.sh para porta 8081)"
echo "========================================"
