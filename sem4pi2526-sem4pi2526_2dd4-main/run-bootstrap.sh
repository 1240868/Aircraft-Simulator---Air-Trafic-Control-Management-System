#!/usr/bin/env bash

# Para o script se houver erro
set -e

# Como o script está na RAIZ, acrescentamos "eapli.base/" aos caminhos
export BASE_CP="eapli.base/exemplo.app.bootstrap/target/exemplo.app.bootstrap-1.4.0-SNAPSHOT.jar:eapli.base/exemplo.app.bootstrap/target/dependency/*"

echo "---------------------------------------------------------"
echo "INICIALIZANDO DADOS DO SISTEMA (BOOTSTRAP)"
echo "---------------------------------------------------------"

# Verificação: Se o JAR não existe, o build não foi feito
if [ ! -f "eapli.base/exemplo.app.bootstrap/target/exemplo.app.bootstrap-1.4.0-SNAPSHOT.jar" ]; then
    echo "ERRO: Ficheiro JAR de Bootstrap não encontrado!"
    echo "Caminho esperado: eapli.base/exemplo.app.bootstrap/target/"
    echo "Por favor, executa primeiro o comando: ./build-all.sh"
    exit 1
fi

# Executa o processo de Bootstrap
java -cp "$BASE_CP" eapli.exemplo.app.bootstrap.ExemploBootstrap

echo "---------------------------------------------------------"
echo "BOOTSTRAP CONCLUÍDO COM SUCESSO"
echo "---------------------------------------------------------"