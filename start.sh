#!/bin/bash
set -euo pipefail

# Prefer a Java 21 runtime (Lombok 1.18.42 is Java-24-safe, but 21 is the
# pinned target). Try the known MS OpenJDK 21 first, then /usr/libexec/java_home.
export JAVA_HOME="${JAVA_HOME:-}"
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  if [ -x "$HOME/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home/bin/java" ]; then
    export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home"
  elif command -v /usr/libexec/java_home >/dev/null 2>&1 && /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  fi
fi
echo "Using JAVA_HOME=${JAVA_HOME:-<default>}"

ROOT="$(cd "$(dirname "$0")" && pwd)"

# Install frontend deps on first run.
if [ ! -d "$ROOT/frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  (cd "$ROOT/frontend" && npm install)
fi

# Start backend in the background.
echo "Starting backend (http://localhost:8080)..."
(cd "$ROOT/backend" && ./mvnw spring-boot:run) &
BACKEND_PID=$!

# Start frontend in the background.
echo "Starting frontend (http://localhost:3000)..."
(cd "$ROOT/frontend" && npm run dev) &
FRONTEND_PID=$!

cleanup() {
  echo "Shutting down..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Backend:  http://localhost:8080"
echo "Frontend: http://localhost:3000"
wait
