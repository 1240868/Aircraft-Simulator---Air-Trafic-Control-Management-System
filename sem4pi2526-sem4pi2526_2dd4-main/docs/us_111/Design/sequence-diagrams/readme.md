# US111 — Diagrama de sequência

- [sd_us_111.puml](sd_us_111.puml) — gerar relatório de simulação (`GenerateSimulationReportUI`, `GenerateSimulationReportController`, `SimulationReportService`, `FileSystem`).

## O que o diagrama representa

O diagrama descreve o **happy path** da US111: um **Flight Control Operator (FCO)** dispara a simulação C diretamente da UI Java e recebe o relatório gerado pelo programa C.

### Actores e participantes

| Elemento | Papel |
|----------|-------|
| **Flight Control Operator** | FCO autenticado na User App |
| **GenerateSimulationReportUI** | Recolhe confirmação, transmite stdout da simulação, apresenta relatório |
| **GenerateSimulationReportController** | Valida autorização; delega ao serviço |
| **AuthorizationPort** | Verifica role `FLIGHT_CONTROL_OPERATOR` |
| **SimulationReportService** | Executa `./flight_sim` via `ProcessBuilder`; lê ficheiro de relatório |
| **ProcessBuilder / Process** | Processo OS do executável C (`scomp.base/flight_sim`) |
| **FileSystem** | `Files.readString("scomp.base/logs/simulation_report.txt")` |

### Fluxo principal

1. FCO seleciona a opção no menu e confirma com `yes`.
2. Controller verifica autorização (`FLIGHT_CONTROL_OPERATOR`).
3. `SimulationReportService` valida que o diretório e o executável existem.
4. `ProcessBuilder` lança `./flight_sim` a partir de `scomp.base/`.
5. Stdout do processo C é transmitido linha a linha para a consola com prefixo `[SIM]`.
6. Após exit code 0, Java lê `scomp.base/logs/simulation_report.txt`.
7. UI apresenta o conteúdo do relatório e o caminho absoluto do ficheiro.

### Decisões de desenho

- **Sem repositório:** não há persistência em RDBMS — o ficheiro gerado pelo C é o artefacto persistente (clarificado pelo professor AMM).
- **Sem agregado de domínio:** `SimulationReportResult` é um Java record sem comportamento; não existe `Simulation` aggregate nesta US.
- **Seamless experience:** o FCO não precisa de compilar ou correr o executável manualmente — Java orquestra tudo.
- **Stderr redirecionado para stdout:** `redirectErrorStream(true)` garante que erros do C também aparecem na consola do operador.

### Cenários alternativos (não desenhados)

| Situação | Comportamento |
|----------|--------------|
| `scomp.base/` não existe | `SimulationReportResult.failure("Simulation directory not found")` |
| `flight_sim` não compilado | `SimulationReportResult.failure("... Compile first: cd scomp.base && make compile")` |
| Processo C termina com erro | `SimulationReportResult.failure("Simulation process exited with code N")` |
| Ficheiro de relatório ausente | `SimulationReportResult.failure("Report file not found after simulation")` |
| FCO cancela com `no` | Simulação não é iniciada |

### Renderizar o diagrama

```bash
plantuml docs/us_111/sequence-diagrams/sd_us_111.puml
```
