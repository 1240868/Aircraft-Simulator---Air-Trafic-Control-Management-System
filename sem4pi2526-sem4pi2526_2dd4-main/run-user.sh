#!/usr/bin/env bash

# O script está agora na RAIZ, por isso temos de acrescentar "eapli.base/"
# aos caminhos para chegar aos ficheiros JAR.
export BASE_CP="eapli.base/exemplo.app.user.console/target/exemplo.app.user.console-1.4.0-SNAPSHOT.jar:eapli.base/exemplo.app.user.console/target/dependency/*"

echo "---------------------------------------------------------"
echo "INICIANDO APLICAÇÃO DE UTILIZADOR (USER APP)"
echo "---------------------------------------------------------"

# Verificação de segurança: Se o build não foi feito, o script avisa.
if [ ! -f "eapli.base/exemplo.app.user.console/target/exemplo.app.user.console-1.4.0-SNAPSHOT.jar" ]; then
    echo "ERRO: Ficheiro JAR não encontrado em eapli.base/exemplo.app.user.console/target/"
    echo "Por favor, executa primeiro o comando: ./build-all.sh"
    exit 1
fi

# Executa a VM do Java
java -cp "$BASE_CP" eapli.exemplo.app.user.console.ExemploUserApp