#!/bin/bash
set -e

echo "Compilando..."
mkdir -p out
javac -d out src/brincadeira/*.java

echo "Copiando recursos para out/..."
cp menino_sprite.png decansando.png aguardando_bola.png basketball.wav out/

echo "Gerando JAR..."
cat > out/MANIFEST.MF << 'EOF'
Main-Class: brincadeira.Main
EOF

jar cfm brincadeira.jar out/MANIFEST.MF -C out .

echo ""
echo "Pronto! Arquivo gerado: brincadeira.jar"
echo "Para executar: java -jar brincadeira.jar"
