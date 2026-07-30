@echo off
cd %~dp0\..
java -cp "out;lib\*" trafficsync.cli.ServerApp server.env
