#!/usr/bin/env bash

# O comando 'set -e' faz com que o script pare se algum erro ocorrer
set -e

echo "---------------------------------------------------------"
echo "SISTEMA INTEGRADO - COMPILAÇÃO GLOBAL"
echo "Certifica-te que o JAVA_HOME aponta para o JDK 21"
echo "Certifica-te que o Maven e o Make estão no PATH"
echo "---------------------------------------------------------"

# 1. Compilar todos os módulos Java (EAPLI + LPROG/ANTLR)
# O Maven na raiz vai ler o POM pai e compilar todos os módulos listados
echo "--- Compilando Módulos Java (EAPLI e LPROG) ---"
mvn $1 package dependency:copy-dependencies surefire-report:report -Daggregate=true

# 2. Compilar os módulos C (SCOMP)
# Entramos na pasta, corremos o make e voltamos para a raiz
echo ""
echo "--- Compilando Módulos C (SCOMP) ---"
if [ -d "scomp.base" ]; then
    cd scomp.base
    # Se o parâmetro for 'clean', limpamos o C também
    if [ "$1" == "clean" ]; then
        make clean
    fi
    make
    cd ..
else
    echo "Erro: Pasta scomp.base não encontrada!"
    exit 1
fi

echo ""
echo "---------------------------------------------------------"
echo "BUILD CONCLUÍDO COM SUCESSO"
echo "---------------------------------------------------------"