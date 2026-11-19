# US111 — Generate a simulation report

## 1. User Story

> **As a Flight Control Operator, I want to receive a summary of the simulation results so that I can determine if the programmed flights are safe to run.**

The FCO triggers an air traffic simulation from the Java application. The system runs the SCOMP C executable, waits for completion, and presents the generated report file to the operator.

---

## 2. Context & Scope

This User Story belongs to the **Simulation & Reporting** context. It bridges **EAPLI (Java)** and **SCOMP (C)**:

- **US109 (SCOMP):** the C process (`flight_sim`) executes the simulation and writes `logs/simulation_report.txt`.
- **US111 (EAPLI):** the FCO launches the simulation seamlessly from the User App and reads the report.

**Clarification from PO (Angelo Martins):**

> *"Don't ask the client how to implement the software. Of course the client would be happy with a seamless experience."*
> → The FCO does not run `./flight_sim` manually; Java invokes it via `ProcessBuilder`.

> *"The user story says that a file should be stored in the file system. I wouldn't call it transient."*
> → The report is persisted on the **file system**, not in the RDBMS (NFR08 does not apply to this artefact).

**Dependencies:** US109 (SCOMP report generation), US100–103 (simulation infrastructure), US030 (authz).

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Flight Control Operator (FCO)** | Primary actor — triggers simulation and reads the report |
| **System (Java)** | Authorizes, launches C process, streams stdout, reads report file |
| **System (C / SCOMP)** | Executes simulation and writes report (US109) |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The FCO must be authenticated in the system |
| PC2 | The FCO must have the `FLIGHT_CONTROL_OPERATOR` role |
| PC3 | The `scomp.base/` directory must exist relative to the working directory |
| PC4 | The `flight_sim` executable must be compiled and executable (`make compile`) |
| PC5 | Flight plan JSON files must be available for the C simulation |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | The C simulation process completes (exit code 0 on success) |
| PS2 | A report file exists at `scomp.base/logs/simulation_report.txt` |
| PS3 | The FCO sees the report content and absolute file path in the UI |
| PS4 | Simulation stdout is streamed to the console prefixed with `[SIM]` |
| PS5 | No domain aggregate is created or modified in the RDBMS |

---

## 6. Main Flow (Basic Course of Events)

1. FCO selects "Generate simulation report" in the User App menu.
2. System verifies authentication and `FLIGHT_CONTROL_OPERATOR` role.
3. UI asks for confirmation; FCO confirms.
4. System validates that `scomp.base/` exists and `flight_sim` is executable.
5. System launches `./flight_sim` via `ProcessBuilder` with working directory `scomp.base/`.
6. System streams C stdout to the console in real time (`[SIM]` prefix).
7. System waits for process termination.
8. System verifies exit code is 0.
9. System reads `logs/simulation_report.txt` from `scomp.base/`.
10. System returns `SimulationReportResult` with content and path.
11. UI displays the report and file location to the FCO.

---

## 7. Alternative Flows

### AF1 — Simulation directory not found
- At step 4, if `scomp.base/` does not exist, the system returns `SimulationReportResult.failure(...)` with an explanatory message.

### AF2 — Executable missing or not executable
- At step 4, if `flight_sim` is absent or not executable, the system returns failure with compile instructions.

### AF3 — Non-zero exit code
- At step 8, if the C process exits with a non-zero code, the system returns failure without reading the report.

### AF4 — Report file missing after simulation
- At step 9, if the report file does not exist, the system returns failure.

### AF5 — I/O or interruption error
- At steps 5–9, `IOException` or `InterruptedException` is caught and returned as failure.

### AF6 — User without required role
- At step 2, if the user lacks `FLIGHT_CONTROL_OPERATOR`, the system throws `AuthzException`.

### AF7 — FCO cancels confirmation
- At step 3, if the FCO does not confirm, the UI returns without invoking the controller.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Only authenticated users with role **FLIGHT_CONTROL_OPERATOR** may generate reports |
| AC2 | The system launches the SCOMP simulation from Java (seamless experience) |
| AC3 | The report file is stored on the file system (`logs/simulation_report.txt`) |
| AC4 | The report includes: total flights, execution status per flight, safety violations (timestamp, positions), overall PASS/FAIL verdict |
| AC5 | Simulation stdout is visible to the operator during execution |
| AC6 | On failure, a clear error message is returned without masking the cause |
| AC7 | No RDBMS persistence is required for the report content |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | Report generation is a read-only operation from the EAPLI domain perspective |
| BR2 | The authoritative report content is produced by SCOMP (US109), not re-computed in Java |
| BR3 | Java is responsible for orchestration (launch, wait, read) only |
| BR4 | The default SCOMP directory is `scomp.base/` relative to the JVM working directory |
| BR5 | A successful run requires C exit code 0 **and** an existing report file |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US100 | Simulation infrastructure — prerequisite |
| US102 | Safety violation detection during simulation |
| US109 | C-side report generation — produces the file consumed by US111 |
| US112 | Monthly statistics report — different scope (calendar aggregation) |
| US030 | Authentication and authorization |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR09 | Authentication and authorisation required |
| NFR10 | Java orchestration layer; C simulation engine |
| NFR08 | **Not applicable** to report file storage (file system, not RDBMS) |

---

## 12. Out of Scope for this US

- Persisting simulation results in the EAPLI database
- Selecting simulation parameters from the UI (uses C defaults / JSON config)
- Listing historical simulation reports
- Monthly or compliance reports (US112 and future types)
- Modifying the C report format (owned by US109)
