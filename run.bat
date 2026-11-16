@echo off
rem Compila e executa a Loja Virtual no Windows. Requer JDK 17 ou superior no PATH.
rem
rem   run.bat            compila e sobe o servidor em http://localhost:8080
rem   run.bat 9090       sobe em outra porta
setlocal
cd /d "%~dp0"
chcp 65001 >nul

if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\loja\dominio\*.java src\loja\servico\*.java src\loja\web\*.java
if errorlevel 1 (
  echo Falha na compilacao. Verifique se o JDK esta instalado: java -version / javac -version
  exit /b 1
)

set PORTA=%~1
if "%PORTA%"=="" set PORTA=8080
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out loja.web.Servidor %PORTA%
