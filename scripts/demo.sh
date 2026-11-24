#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build
javac -encoding UTF-8 -d build backend/src/main/java/com/youtrace/core/IncidentEngine.java demo/LocalServer.java
java -cp build LocalServer "${1:-8080}"
