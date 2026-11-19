#!/usr/bin/env bash

# Para o script se houver erro
set -e

echo "---------------------------------------------------------"
echo "REBUILD GLOBAL (Limpando e Compilando do zero)"
echo "---------------------------------------------------------"

# Chama o build-all.sh que já configurámos na raiz, passando o parâmetro 'clean'
# O clean vai limpar os targets do Maven e os ficheiros .o do Make (C)
./build-all.sh clean

echo "---------------------------------------------------------"
echo "REBUILD CONCLUÍDO COM SUCESSO"
echo "---------------------------------------------------------"