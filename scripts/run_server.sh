#!/bin/bash
cd "$(dirname "$0")/.."
java -cp "out:lib/*" trafficsync.cli.ServerApp server.env
