#!/usr/bin/env bash
# Virtual Classroom Platform - Local Dev Startup Script for Unix/macOS/Linux
set -e

echo "=================================================="
echo " Starting Virtual Classroom Platform Local Stack "
echo "=================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

if command -v docker &> /dev/null; then
    echo "[1/3] Starting PostgreSQL service via Docker Compose..."
    cd "$SCRIPT_DIR/../docker"
    docker compose up -d db
    cd "$ROOT_DIR"
else
    echo "[1/3] Docker not detected. Please ensure local PostgreSQL is running on port 5432."
fi

echo ""
echo "To start the backend in a new terminal:"
echo "  cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"
echo ""
echo "To start the frontend in another terminal:"
echo "  cd frontend && npm run dev"
echo ""
