# US112 — Analysis

## 1. Overview

Analysis for **US112 — Generate a monthly statistics report**. The FCO generates a calendar-month statistics report for their air control area, using a **Template Method** + **Strategy** reporting framework designed for future extension.

Related documentation: [requirements/requisitos.md](../requirements/requisitos.md), [sequence-diagrams/readme.md](../Design/sequence-diagrams/readme.md), [class-diagrams/readme.md](../Design/class-diagrams/readme.md).

Diagrams: [sequence-diagrams/sd_us_112.puml](../Design/sequence-diagrams/sd_us_112.puml), [class-diagrams/US112-class-diagram.puml](../Design/class-diagrams/US112-class-diagram.puml).

**Status:** implemented.

---

## 2. Actor

| Actor | Description |
|-------|-------------|
| **Flight Control Operator (FCO)** | Authenticated user with role `FLIGHT_CONTROL_OPERATOR`, linked to an air control area via collaborator username |

---

## 3. Domain concepts

| Concept | Type | Justification |
|---------|------|---------------|
| `Report` | **Abstract class (Template Method)** | Defines report skeleton: header, sections, footer |
| `MonthlyStatisticsReport` | **Concrete report** | Monthly statistics implementation |
| `ReportSection` | **Value Object** | Title + lines for one report section |
| `ReportDataCollector<T>` | **Strategy interface** | Abstracts data gathering per report type |
| `MonthlyReportDataCollector` | **Concrete Strategy** | Queries flight plans and aggregates counts |
| `MonthlyReportStats` | **Value Object** | Immutable statistics snapshot |
| `MonthlyReportResult` | **Application record** | Success + file path |
| `ReportExporter` | **Infrastructure port** | Writes report to file system |
| `TextFileReportExporter` | **Infrastructure adapter** | `.txt` export implementation |
| `FlightPlan` | **Aggregate Root (external)** | Queried read-only |
| `AirControlArea` | **Aggregate Root (external)** | Resolved by collaborator username |

---

## 4. Design patterns

### 4.1 Template Method — `Report`

```java
abstract class Report {
    String render() {
        // skeleton: header + sections + footer
    }
    abstract String title();
    abstract List<ReportSection> sections();
}
```

`MonthlyStatisticsReport` implements only `title()` and `sections()`. Future report types (compliance, incident) add new subclasses without changing the template.

### 4.2 Strategy — `ReportDataCollector<T>`

```java
interface ReportDataCollector<T> {
    T collect();
}
```

`MonthlyReportDataCollector` implements `collect()` by querying `FlightPlanRepository.findByDepartureYearMonth(year, month)` and counting by `FlightPlanStatus`.

**PO confirmation:** Strategy pattern is the intended extension point for different data sources.

---

## 5. Data aggregation

```
MonthlyReportDataCollector.collect()
        │
        ▼
FlightPlanRepository.findByDepartureYearMonth(year, month)
        │
        ▼
for each FlightPlan:
    total++
    if APPROVED  → approved++
    if REJECTED  → rejected++
    if DRAFT     → draft++
        │
        ▼
new MonthlyReportStats(total, approved, rejected, draft)
```

| Metric | Source |
|--------|--------|
| `totalFlightPlans` | Count of all plans in month |
| `approved` | `FlightPlanStatus.APPROVED` |
| `rejected` | `FlightPlanStatus.REJECTED` |
| `draft` | `FlightPlanStatus.DRAFT` |

---

## 6. Area resolution

The FCO **does not select** an area in the UI. The controller resolves it automatically:

```java
areaRepo.findByCollaboratorUsername(authz.currentUsername())
```

This matches the PO clarification: the area is implicit from the authenticated FCO.

---

## 7. Application-level rules

| # | Check | Where | On failure |
|---|-------|-------|------------|
| 1 | Role `FLIGHT_CONTROL_OPERATOR` | `GenerateMonthlyReportController.generate()` | `AuthzException` |
| 2 | FCO linked to an area | `findByCollaboratorUsername` | `IllegalStateException` |
| 3 | Valid year/month | UI parsing / `YearMonth.of` | UI error message |
| 4 | Collect statistics | `MonthlyReportDataCollector.collect()` | — |
| 5 | Build report | `MonthlyStatisticsReport` constructor | — |
| 6 | Export to file | `TextFileReportExporter.export()` | IOException |
| 7 | Read-only | No repository `save` calls | — |

---

## 8. Repository extensions

| Repository | New method | Purpose |
|------------|------------|---------|
| `FlightPlanRepository` | `findByDepartureYearMonth(int year, int month)` | Filter plans by departure calendar month |
| `AirControlAreaRepository` | `findByCollaboratorUsername(String username)` | Resolve FCO's area |

Implementations: `InMemoryFlightPlanRepository`, `JpaFlightPlanRepository`, `InMemoryAirControlAreaRepository`, `JpaAirControlAreaRepository`.

---

## 9. Layer responsibilities

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `GenerateMonthlyReportUI` | Collect year/month; display result path |
| **Application** | `GenerateMonthlyReportController` | Auth, area resolution, orchestration |
| **Application** | `MonthlyReportDataCollector` | Strategy — aggregate flight plan stats |
| **Domain** | `Report`, `MonthlyStatisticsReport`, VOs | Report structure and statistics |
| **Infrastructure** | `TextFileReportExporter` | Write `.txt` file to `reports/` |
| **Infrastructure** | Repositories | Read-only queries |

---

## 10. Key design decisions

| Decision | Justification |
|----------|---------------|
| Template Method on `Report` | PO requirement: foundational framework for future report types |
| Strategy on `ReportDataCollector` | PO explicitly confirmed Strategy pattern |
| Area implicit from username | PO clarification; simpler UI |
| Calendar month via `YearMonth` | PO clarification: strict calendar month |
| Read-only operation | No new persistent aggregate; snapshot exported to file |
| `.txt` export sufficient | PO clarification; charts optional later |
| `MonthlyReportStats` as immutable VO | Safe to pass between collector and report builder |

---

## 11. Relation to US111

| Aspect | US111 | US112 |
|--------|-------|-------|
| Scope | Single C simulation run | Calendar month for one area |
| Data source | SCOMP file (`report.c`) | EAPLI repositories |
| Pattern | Simple service + ProcessBuilder | Template Method + Strategy |
| Persistence | File system only | Reads RDBMS; writes file |

US112 establishes the **reporting framework** that future US should reuse; US111 remains independent (C integration).

---

## 12. Functional dependencies

| US | Relationship |
|----|-------------|
| US050/061 | Area and FCO collaborator |
| US080 | Flight plans as data source |
| US111 | Complementary simulation report |
| US030 | Authentication and authorization |
