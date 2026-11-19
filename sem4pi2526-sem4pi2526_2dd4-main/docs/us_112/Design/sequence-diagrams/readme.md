# US112 — Diagrama de sequência

- [sd_us_112.puml](sd_us_112.puml)

## O que o diagrama representa

O diagrama descreve o **happy path** da US112: um **Flight Control Operator (FCO)** gera um relatório mensal de estatísticas para a sua área de controlo aéreo.

---

## Actores e participantes

| Elemento | Papel |
|----------|-------|
| **Flight Control Operator** | Actor autenticado com role `FLIGHT_CONTROL_OPERATOR` |
| **GenerateMonthlyReportUI** | Recolhe ano e mês via consola |
| **GenerateMonthlyReportController** | Autorização, resolução da área, orquestração |
| **AuthorizationPort** | Verifica role e fornece username da sessão |
| **AirControlAreaRepository** | Resolve a área do FCO por username |
| **MonthlyReportDataCollector** | Estratégia concreta de recolha de dados (Strategy) |
| **FlightPlanRepository** | Fonte de dados — planos de voo do mês |
| **MonthlyStatisticsReport** | Objecto de domínio com estatísticas agregadas |
| **TextFileReportExporter** | Escreve o ficheiro `.txt` para disco |

---

## Fluxo principal

1. FCO indica **ano** e **mês** (validação de intervalo na UI).
2. Controller verifica role `FLIGHT_CONTROL_OPERATOR`.
3. Controller obtém o username da sessão e consulta a área correspondente via `findByCollaboratorUsername`.
4. Controller cria um `MonthlyReportDataCollector` (Strategy) e invoca `collect()`.
5. O collector consulta `FlightPlanRepository.findByDepartureYearMonth(year, month)` e agrega contagens por `FlightPlanStatus`.
6. Controller constrói `MonthlyStatisticsReport` (Template Method — render próprio, branding partilhado).
7. `TextFileReportExporter` invoca `report.render()` e escreve o ficheiro em `reports/monthly_report_*.txt`.
8. UI informa o FCO com o caminho do ficheiro gerado.

---

## Decisões de desenho

| Decisão | Justificação |
|---------|-------------|
| A área não é seleccionada pelo FCO | Confirmado pelo PO: FCO só acede à sua área |
| `ReportDataCollector` como Strategy | Exigido pelo PO — cada tipo de relatório recolhe dados de forma diferente |
| `Report` como Template Method | Garante branding e estrutura consistentes em todos os relatórios futuros |
| Sem `SimulationRepository` | Dado que não há simulações persistidas, a fonte de dados é `FlightPlanRepository` |
| Formato `.txt` | Confirmado pelo PO como suficiente |
| `MonthlyReport` não é persistido | Snapshot gerado on-demand; operação read-only |

---

## Renderizar o diagrama

```bash
plantuml docs/us_112/sequence-diagrams/sd_us_112.puml
```
