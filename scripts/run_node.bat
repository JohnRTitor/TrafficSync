@echo off
if "%~1"=="" (
  echo Usage: run_node.bat ^<env_file^>
  exit /b 1
)
cd %~dp0\..
java -cp "out;lib\*" trafficsync.cli.NodeApp %~1
