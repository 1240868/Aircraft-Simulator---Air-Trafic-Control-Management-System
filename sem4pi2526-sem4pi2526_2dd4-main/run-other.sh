#!/usr/bin/env bash

# O script está agora na RAIZ, por isso temos de acrescentar "eapli.base/"
# aos caminhos para chegar aos ficheiros JAR.
export BASE_CP="eapli.base/exemplo.app.other.console/target/exemplo.app.other.console-1.4.0-SNAPSHOT.jar:eapli.base/exemplo.app.other.console/target/dependency/*"

echo "---------------------------------------------------------"
echo "INICIANDO APLICAÇÃO OUTRA (OTHER APP)"
echo "---------------------------------------------------------"

# Verificação de segurança: Se o build não foi feito, o script avisa.
if [ ! -f "eapli.base/exemplo.app.other.console/target/exemplo.app.other.console-1.4.0-SNAPSHOT.jar" ]; then
    echo "ERRO: Ficheiro JAR não encontrado em eapli.base/exemplo.app.other.console/target/"
    echo "Por favor, executa primeiro o comando: ./build-all.sh"
    exit 1
fi

# Executa a VM do Java
java -cp "$BASE_CP" eapli.exemplo.app.other.console.OtherApp