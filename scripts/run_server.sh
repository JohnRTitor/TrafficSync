#!/bin/bash
cd "$(dirname "$0")/.."
java -cp out trafficsync.cli.ServerApp server.env
