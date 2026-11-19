#!/usr/bin/env bash

# Para o script se houver erro
set -e

echo "---------------------------------------------------------"
echo "QUICK BUILD GLOBAL (Sem Testes e Javadoc)"
echo "---------------------------------------------------------"

# Executa o Maven na raiz para compilar EAPLI e LPROG
# Adicionei -DskipTests para ser realmente "quick"
mvn -B $1 dependency:copy-dependencies verify -Dmaven.javadoc.skip=true -DskipTests

echo "---------------------------------------------------------"
echo "QUICK BUILD CONCLUÍDO"
echo "---------------------------------------------------------"