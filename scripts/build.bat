@echo off
cd %~dp0\..
if not exist out mkdir out
dir /s /B src\*.java > sources.txt
javac -cp "lib\*" @sources.txt -d out
del sources.txt
echo Build complete.
