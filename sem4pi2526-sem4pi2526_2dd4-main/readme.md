
# Project AIControl - AISafe Flight Control System

## 1. Description of the Project
Este projeto consiste no desenvolvimento de um sistema de gestão de controlo de tráfego aéreo para a startup **AISafe**. O sistema é uma solução integrada que abrange o back-office para gestão de modelos de aeronaves, aeroportos, planos de voo e simulação de movimentos aéreos em tempo real.

O projeto é desenvolvido no âmbito do 4º Semestre da Licenciatura em Engenharia Informática (ISEP), integrando conhecimentos de:
*   **EAPLI:** Design de software usando Domain-Driven Design (DDD) e persistência de dados.
*   **LPROG:** Criação de uma linguagem específica (DSL) para planos de voo usando ANTLR.
*   **SCOMP / LAPR4:** Simulação de voos em ambiente Unix utilizando processos, memória partilhada e sinais em C.
*   **RCOMP:** Comunicação em rede TCP/UDP para acesso remoto.

## 2. Planning and Technical Documentation
[Planning and Technical Documentation](docs/readme.md)

## 3. How to Build
O projeto utiliza uma estrutura multi-módulo (Java/Maven e C/Make). Para compilar todos os componentes de uma só vez (EAPLI, LPROG e SCOMP), utiliza os scripts na raiz:

**Pré-requisitos:** Java 21, Maven e GCC/Make instalados.

*   **Compilação Completa:**
    ```bash
    ./build-all.sh
    ```
*   **Limpar e Recompilar (Clean Build):**
    ```bash
    ./rebuild-all.sh
    ```
*   **Compilação Rápida (Sem testes):**
    ```bash
    ./quickbuild.sh
    ```

## 4. How to Execute Tests
A qualidade do código é monitorizada através de testes unitários e cobertura de código (mínimo 90% exigido pela NFR03).

*   **Executar Testes:**
    ```bash
    ./run-tests.sh
    ```
*   **Relatório de Cobertura:** Após a execução, o relatório JaCoCo estará disponível em: `eapli.base/target/site/jacoco/index.html`

## 5. How to Run
Antes de iniciar as aplicações, é necessário popular a base de dados com os dados iniciais.

1.  **Carregar Dados Iniciais (Bootstrap):**
    ```bash
    ./run-bootstrap.sh
    ```
2.  **Iniciar Aplicação Backoffice (Admin/Operadores):**
    ```bash
    ./run-backoffice.sh
    ```
3.  **Iniciar Aplicação User (Pilotos/Colaboradores):**
    ```bash
    ./run-user.sh
    ```

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)
Graças à implementação da **US005**, o deployment em máquinas Unix-compatible (Linux/macOS) é simplificado:

1.  Clonar o repositório.
2.  Garantir permissões de execução nos scripts: `chmod +x *.sh`.
3.  Executar `./build-all.sh` para preparar o ambiente.
4.  O sistema está pronto a ser utilizado sem necessidade de configurações manuais complexas.

O sistema suporta persistência configurável (H2 In-memory ou RDBMS) através do ficheiro `application.properties` no módulo de persistência.

## 7. How to Generate PlantUML Diagrams
Para gerar os diagramas de documentação (essencial para a NFR02), executa:

    ./generate-plantuml-diagrams.sh

---

### Notas de Configuração (Checklist Final):
1.  **Permissões:** Não te esqueças de correr `chmod +x *.sh` na raiz antes de testares.
2.  **JDK:** Garante que o teu IDE e terminal estão a usar o **Java 21**.
3.  **GitHub Actions:** Este README complementa o workflow de CI configurado na **US004**, garantindo que qualquer membro da equipa ou avaliador consegue replicar os resultados localmente.