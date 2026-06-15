#!/bin/bash
# Inicia os três serviços em terminais separados (Linux/WSL)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Iniciando serviços..."
echo "  ms_user  -> http://localhost:8081"
echo "  ms_email -> http://localhost:8082"
echo "  frontend -> http://localhost:3000"
echo ""

if command -v gnome-terminal &> /dev/null; then
    gnome-terminal --title="ms_user"  -- bash -c "cd '$ROOT_DIR/ms_user'  && ./mvnw spring-boot:run; exec bash"
    gnome-terminal --title="ms_email" -- bash -c "cd '$ROOT_DIR/ms_email' && ./mvnw spring-boot:run; exec bash"
    gnome-terminal --title="frontend"  -- bash -c "cd '$ROOT_DIR/frontend' && npm install && npm start; exec bash"
elif command -v wt.exe &> /dev/null; then
    wt.exe -w 0 new-tab --title "ms_user"  bash -c "cd '$ROOT_DIR/ms_user'  && ./mvnw spring-boot:run; exec bash" \; \
           new-tab --title "ms_email" bash -c "cd '$ROOT_DIR/ms_email' && ./mvnw spring-boot:run; exec bash" \; \
           new-tab --title "frontend"  bash -c "cd '$ROOT_DIR/frontend' && npm install && npm start; exec bash"
else
    echo "Abrindo em background (sem terminal gráfico detectado)..."
    (cd "$ROOT_DIR/ms_user"  && ./mvnw spring-boot:run) &
    (cd "$ROOT_DIR/ms_email" && ./mvnw spring-boot:run) &
    (cd "$ROOT_DIR/frontend" && npm install && npm start) &
    echo "Serviços iniciados em background."
fi
