#!/usr/bin/env bash
set -euo pipefail

echo "[init] running tests"
mvn -q test

echo "[init] starting app for health check"
mvn -q spring-boot:run > /tmp/manju-init.log 2>&1 &
APP_PID=$!

cleanup() {
  if kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID"
  fi
}
trap cleanup EXIT

sleep 12
curl -fsS http://localhost:8080/api/health
echo
echo "[init] done"
