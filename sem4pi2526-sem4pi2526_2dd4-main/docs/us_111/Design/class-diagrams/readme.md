# US111 — Diagrama de classes

- [US111-class-diagram.puml](US111-class-diagram.puml) — gerar relatório de simulação (`GenerateSimulationReportController`, `SimulationReportService`, `SimulationReportResult`).

## O que o diagrama representa

O diagrama modela a **camada EAPLI** da US111: o FCO dispara a simulação C a partir da UI Java e recebe o relatório gerado pelo programa C sem qualquer persistência em base de dados.

### Camadas e pacotes

| Pacote | Conteúdo | Responsabilidade |
|--------|----------|-----------------|
| **presentation** | `GenerateSimulationReportUI` | Menu, confirmação, transmissão de stdout, apresentação do relatório |
| **application** | `GenerateSimulationReportController` | Autorização + orquestração |
| **application** | `SimulationReportService` | Executa `./flight_sim`, lê ficheiro |
| **application** | `SimulationReportResult` | Record Java — resultado do serviço |
| **application.security** | `AuthorizationPort`, `FrameworkAuthorizationAdapter` | Porto de autorização (testável) |
| **JDK** | `ProcessBuilder`, `Process`, `Files` | Execução do processo C e leitura do ficheiro |

### Decisões de arquitetura

**Sem agregado de domínio:**
O relatório é read-only, gerado pelo C, lido uma vez e mostrado. Não há invariantes a proteger, ciclo de vida a gerir, nem persistência em BD. Criar um agregado `Simulation` seria apenas burocracia sem valor.

**`SimulationReportResult` como Java record:**
Transporta os dados do serviço para a UI de forma imutável e type-safe. Tem dois factory methods — `success()` e `failure()` — que tornam o estado explícito sem exceções.

**`SimulationReportService` testável via subclasse:**
O método `generateReport()` não é `final`, permitindo que os testes usem um `StubService` que devolve um resultado fixo sem correr o executável C.

**`AuthorizationPort` como interface:**
Segue o padrão do projeto (hexagonal). Os testes injectam um `FakeAuth` sem precisar do `AuthzRegistry`.

### Separação EAPLI / SCOMP

| US | Módulo | O que produz |
|----|--------|-------------|
| **US109** | SCOMP (`report.c`, thread C) | `scomp.base/logs/simulation_report.txt` |
| **US111** | EAPLI (Java) | Lê e apresenta o ficheiro acima; o ficheiro é o artefacto persistente |

### Renderizar o diagrama

```bash
plantuml docs/us_111/class-diagrams/US111-class-diagram.puml
```
