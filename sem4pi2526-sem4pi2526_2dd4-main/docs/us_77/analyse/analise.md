# US077 — Analysis

## 1. Overview

Domain analysis for **US077 — Remove (deactivate) a pilot**. The operation is a **command** (write): the ATCC changes a pilot's status from `ACTIVE` to `INACTIVE` within their company roster.

Related documentation: [requirements/requisitos.md](../requirements/requisitos.md), [design/design.md](../design/design.md).  
Diagrams: [design/cd_us_77.puml](../design/cd_us_77.puml), [design/sd_us_77.puml](../design/sd_us_77.puml).

**Status:** implemented.

---

## 2. Actor

| Actor | Description |
|-------|-------------|
| **Air Transport Company Collaborator (ATCC)** | Authenticated collaborator with role `AIR_TRANSPORT_COMPANY_COLLABORATOR` who manages the company's pilot roster |

---

## 3. Domain concepts

| Concept | Type | Justification |
|---------|------|---------------|
| `Pilot` | **Aggregate Root** | Entity whose status is changed; enforces deactivation invariants |
| `PilotStatus` | **Enum** | `ACTIVE` → `INACTIVE` transition |
| `PilotId` | **Value Object** | Identifies the pilot to deactivate |
| `CompanyId` | **Value Object (ref)** | Scope — pilot must belong to ATCC's company |
| `FlightPlan` | **Aggregate (ref)** | Cross-aggregate check — assigned plans block deactivation |

---

## 4. Aggregate design — Pilot

```
Pilot (Aggregate Root)
├── PilotId
├── CompanyId              ← reference by identity (scope check)
├── PilotStatus            ← ACTIVE → INACTIVE (US077)
└── deactivate(hasAssignedFlightPlans)  ← US077 invariant
```

### Invariants (tested in `DeactivatePilotTest`)

| ID | Invariant | Test |
|----|-----------|------|
| INV-1 | `ACTIVE` pilot without assigned flight plans → `INACTIVE` | `deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans` |
| INV-1 | Assigned flight plans block deactivation | `deactivateShouldRejectWhenPilotHasAssignedFlightPlans` |
| INV-2 | Only `ACTIVE` pilots can be deactivated | `deactivateShouldRejectWhenPilotIsNotActive` |
| INV-2 | Already `INACTIVE` cannot be deactivated again | `deactivateShouldRejectWhenPilotAlreadyInactive` |

### Cross-aggregate references

- `Pilot` → `CompanyId` (company scope; does not modify `AirTransportCompany`)
- `Pilot` → `FlightPlan` (checked via `FlightPlanRepository.hasAssignedFlightPlansFor(PilotId)` — reference by identity, read-only query)

### Downstream effects (US080)

- `RegisterFlightPlanController.listPilots()` returns only `ACTIVE` pilots
- `FlightPlanFactory.pilotIsActive()` rejects inactive pilots when registering new flight plans

---

## 5. Application-level rules (tested in `DeactivatePilotControllerTest`)

| # | Check | Where | On failure |
|---|-------|-------|------------|
| 1 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `deactivatePilot()` — `AuthorizationPort` | `AuthzException` |
| 2 | ATCC linked to a `CompanyId` | `PilotRosterListing.requireCompanyOfUser` | `IllegalStateException` |
| 3 | Pilot exists | `PilotRepository.ofIdentity` | `IllegalArgumentException` |
| 4 | Pilot belongs to ATCC's company | `DeactivatePilotController` | `IllegalArgumentException` |
| 5 | No assigned flight plans | `FlightPlanRepository` + `Pilot.deactivate` | `IllegalStateException` |
| 6 | Persist updated pilot | `PilotRepository.save` | — |
