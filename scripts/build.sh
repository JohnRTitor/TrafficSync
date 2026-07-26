#!/bin/bash
cd "$(dirname "$0")/.."
mkdir -p out
javac $(find src -name "*.java") -d out
echo "Build complete."
