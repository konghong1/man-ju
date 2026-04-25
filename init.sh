#!/usr/bin/env bash
set -euo pipefail

echo "[init] running backend tests"
mvn -q test

echo "[init] building frontend"
pushd frontend >/dev/null
npm ci
npm run build
popd >/dev/null

echo "[init] starting backend"
mvn -q spring-boot:run >/tmp/manju-backend.log 2>&1 &
BACKEND_PID=$!

echo "[init] starting frontend preview"
(
  cd frontend
  npm run preview -- --host 127.0.0.1 --port 4173
) >/tmp/manju-frontend.log 2>&1 &
FRONTEND_PID=$!

cleanup() {
  if kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID"
  fi
  if kill -0 "$FRONTEND_PID" 2>/dev/null; then
    kill "$FRONTEND_PID"
  fi
}
trap cleanup EXIT

sleep 16
curl -fsS http://localhost:8080/api/health
echo
curl -fsS http://127.0.0.1:4173 >/dev/null
echo "[init] frontend health check passed"
echo "[init] done"
