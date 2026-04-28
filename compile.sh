#!/bin/bash
set -e
mkdir -p out
javac -d out src/brincadeira/*.java
echo "Compilação concluída com sucesso."
