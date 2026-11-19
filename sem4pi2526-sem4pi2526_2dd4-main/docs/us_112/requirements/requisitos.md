# US112 — Generate a monthly statistics report

## 1. User Story

> **As a Flight Control Operator, I want to generate a monthly statistics report.**

The FCO generates a consolidated statistics report for their **air control area** covering a full **calendar month**. The report aggregates flight plan data from the repository and is exported to a text file on disk.

---

## 2. Context & Scope

This User Story belongs to the **Reporting** bounded context of the AISafe system. It establishes a **foundational reporting framework** (Template Method + Strategy) extensible to future report types (compliance, incident, etc.).

**Clarifications from PO (Angelo Martins):**

| Question | Answer |
|----------|--------|
| Period | Strict calendar month (1st to last day) |
| Area | Implicit from authenticated FCO — **not selectable** in UI |
| Data source | Repository (database) — team choice |
| Key pattern | **Strategy** — explicitly confirmed by PO |
| Format | `.txt` file sufficient; charts optional |

The operation is **read-only**: no aggregate is created or modified in the database.

**Dependencies:** US050 (air control areas), US061 (area collaborator), US080 (flight plans), US030 (authz).

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Flight Control Operator (FCO)** | Primary actor — requests monthly statistics for their area |
| **System** | Resolves area, aggregates data, generates and exports report file |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The FCO must be authenticated in the system |
| PC2 | The FCO must have the `FLIGHT_CONTROL_OPERATOR` role |
| PC3 | The FCO must be linked to an **Air Control Area** (via collaborator username) |
| PC4 | Flight plan data may exist for the selected year/month (empty month is valid) |
| PC5 | Persistence infrastructure must be configured |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | A report file is written to disk (e.g. `reports/monthly_report_*.txt`) |
| PS2 | The file path is returned to the FCO |
| PS3 | Statistics reflect flight plans with departure in the selected calendar month |
| PS4 | No domain aggregate is modified |
| PS5 | The reporting framework remains extensible for future report types |

---

## 6. Main Flow (Basic Course of Events)

1. FCO selects "Generate monthly statistics report" in the User App menu.
2. System verifies authentication and `FLIGHT_CONTROL_OPERATOR` role.
3. FCO provides **year** and **month**.
4. System resolves the FCO's air control area via `AirControlAreaRepository.findByCollaboratorUsername(username)`.
5. System invokes `MonthlyReportDataCollector` (Strategy) to query `FlightPlanRepository.findByDepartureYearMonth(year, month)`.
6. System aggregates counts: total, approved, rejected, draft.
7. System builds `MonthlyStatisticsReport` (Template Method subclass of `Report`).
8. System exports the report via `TextFileReportExporter`.
9. System returns `MonthlyReportResult` with the file path.
10. UI displays the path to the FCO.

---

## 7. Alternative Flows

### AF1 — FCO not linked to any area
- At step 4, if no area is found for the username, the system throws `IllegalStateException`.

### AF2 — Invalid year or month
- At step 3, if parsing fails or month is out of range (1–12), the UI presents an error and returns.

### AF3 — No flight plans in the period
- At step 6, all counts are zero; the report is still generated with empty statistics.

### AF4 — Export failure
- At step 8, if file write fails, the exception propagates to the UI.

### AF5 — User without required role
- At step 2, if the user lacks `FLIGHT_CONTROL_OPERATOR`, the system throws `AuthzException`.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Only authenticated users with role **FLIGHT_CONTROL_OPERATOR** may generate monthly reports |
| AC2 | The air control area is resolved automatically from the authenticated FCO's username |
| AC3 | The FCO specifies year and month; the system covers the full calendar month |
| AC4 | Statistics include: total flight plans, approved, rejected, draft |
| AC5 | The report is saved as a text file on disk |
| AC6 | The operation is read-only — no aggregate is modified |
| AC7 | Architecture uses **Strategy** (`ReportDataCollector`) and **Template Method** (`Report`) for extensibility |
| AC8 | Authentication and authorisation are enforced |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | The FCO cannot select a different area — scope is always their linked area |
| BR2 | Calendar month boundaries: departures filtered by `YearMonth` of `departureDateTime` |
| BR3 | Counts are by `FlightPlanStatus`: `APPROVED`, `REJECTED`, `DRAFT` (other statuses counted in total only) |
| BR4 | Report content follows a fixed template: header, sections, footer (Template Method) |
| BR5 | New report types must extend `Report` and implement a new `ReportDataCollector` without modifying existing code (Open/Closed) |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US050 | Air control areas — prerequisite |
| US061 | Area collaborator — links FCO username to area |
| US080 | Flight plans — data source for statistics |
| US111 | Simulation report — different scope (single C simulation event) |
| US030 | Authentication and authorization |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% for domain and controller packages |
| NFR08 | Flight plan data read from configured persistence |
| NFR09 | Authentication and authorisation required |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Simulation statistics (US111)
- Graphical/chart output (optional future enhancement)
- Selecting area manually in UI
- Persisting generated reports in RDBMS
- Compliance or incident report types (future subclasses of `Report`)
