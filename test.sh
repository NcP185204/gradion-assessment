#!/bin/bash
set -euo pipefail

# Prefer a Java 21 runtime, same detection as start.sh.
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

# Install frontend deps if missing (tests need the toolchain).
if [ ! -d "$ROOT/frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  (cd "$ROOT/frontend" && npm install)
fi

echo "Running backend tests..."
(cd "$ROOT/backend" && ./mvnw test)

echo ""
echo "Running frontend tests..."
(cd "$ROOT/frontend" && npm test)
