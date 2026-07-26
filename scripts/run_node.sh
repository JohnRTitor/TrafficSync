#!/bin/bash
if [ -z "$1" ]; then
  echo "Usage: ./run_node.sh <env_file>"
  exit 1
fi
cd "$(dirname "$0")/.."
java -cp out trafficsync.cli.NodeApp $1
