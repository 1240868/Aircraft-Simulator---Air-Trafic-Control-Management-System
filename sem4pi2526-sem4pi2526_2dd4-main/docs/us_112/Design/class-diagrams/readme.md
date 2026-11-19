# US112 — Diagrama de classes

- [US112-class-diagram.puml](US112-class-diagram.puml)

## O que o diagrama representa

O diagrama modela a implementação da US112 na framework EAPLI. Um **Flight Control Operator (FCO)** gera um relatório mensal de estatísticas para a sua área de controlo aéreo.

A implementação assenta em dois padrões estruturais exigidos pelo enunciado:

---

### Template Method — `Report`

`Report` é uma classe abstracta que define o **esqueleto de todos os relatórios** do sistema (branding, cabeçalho, rodapé). Subclasses implementam `title()` e `sections()`.

```
Report (abstract)
  └── MonthlyStatisticsReport   ← relatório mensal concreto
```

Quando for necessário criar um relatório de *compliance* ou *incident*, basta criar uma nova subclasse de `Report` — sem tocar no código existente.

---

### Strategy — `ReportDataCollector<T>`

Cada tipo de relatório tem uma forma própria de recolher dados. `ReportDataCollector<T>` é a interface Strategy. A implementação concreta para a US112 é `MonthlyReportDataCollector`, que consulta o `FlightPlanRepository` por ano e mês.

```
ReportDataCollector<T>  (interface)
  └── MonthlyReportDataCollector  ← consulta FlightPlanRepository
```

---

### Camadas

| Pacote | Conteúdo |
|--------|----------|
| `user_presentation` | `GenerateMonthlyReportUI` |
| `reporting_application` | `GenerateMonthlyReportController`, `MonthlyReportDataCollector`, `ReportExporter`, `TextFileReportExporter`, `MonthlyReportResult` |
| `reporting_domain` | `Report` (abstract), `MonthlyStatisticsReport`, `MonthlyReportStats`, `ReportSection`, `ReportDataCollector<T>` |
| `repositories` | `FlightPlanRepository`, `AirControlAreaRepository` |

---

### Resolução da área

O FCO **não selecciona** a área de controlo. Esta é resolvida automaticamente a partir do username autenticado via `AirControlAreaRepository.findByCollaboratorUsername(username)`. Decisão confirmada pelo PO: um FCO só pode gerar relatórios para a área onde trabalha.

---

### Queries adicionadas aos repositórios

| Repositório | Método |
|-------------|--------|
| `FlightPlanRepository` | `findByDepartureYearMonth(int year, int month)` |
| `AirControlAreaRepository` | `findByCollaboratorUsername(String username)` |

---

### Renderizar o diagrama

```bash
plantuml docs/us_112/class-diagrams/US112-class-diagram.puml
```
