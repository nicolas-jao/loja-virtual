#!/usr/bin/env bash
# Compila e executa a Loja Virtual. Requer apenas o JDK 17 ou superior (testado com o 21).
#
#   ./run.sh            compila e sobe o servidor em http://localhost:8080
#   ./run.sh 9090       sobe em outra porta
set -euo pipefail
cd "$(dirname "$0")"

rm -rf out && mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out loja.web.Servidor "${1:-8080}"
