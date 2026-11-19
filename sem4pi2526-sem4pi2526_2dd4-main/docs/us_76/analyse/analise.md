# US076 — Analysis

## 1. Overview

Domain analysis for **US076 — List company pilot roster**. The operation is a **query** (read-only): the ATCC views active pilots registered in their air transport company.

Related documentation: [requirements/requisitos.md](../requirements/requisitos.md), [design/design.md](../design/design.md).  
Diagrams: [design/cd_us_76.puml](../design/cd_us_76.puml), [design/sd_us_76.puml](../design/sd_us_76.puml).

**Status:** implemented.

---

## 2. Actor

| Actor | Description |
|-------|-------------|
| **Air Transport Company Collaborator (ATCC)** | Authenticated collaborator of an air transport company with role `AIR_TRANSPORT_COMPANY_COLLABORATOR` |

---

## 3. Domain concepts

| Concept | Type | Justification |
|---------|------|---------------|
| `Pilot` | **Aggregate Root** | Queried entity; defines roster eligibility |
| `PilotStatus` | **Enum** | Filters `ACTIVE` pilots vs excluded states |
| `PilotId` | **Value Object** | Pilot identity |
| `CompanyId` | **Value Object (ref)** | Listing scope — pilots of the ATCC's company |
| `FullName`, `Email`, `License`, `PilotCertification` | **Value Objects** | Data displayed in the roster |
| `AircraftModelId` | **Value Object (ref)** | Certified models; labels resolved in application layer via `AircraftModelRepository` |

---

## 4. Aggregate design — Pilot

```
Pilot (Aggregate Root)
├── PilotId
├── FullName, Email
├── CompanyId              ← reference by identity (roster scope)
├── PilotCertification, License
├── PilotStatus
├── certifiedModels        → List<AircraftModelId> (refs)
└── isEligibleForRosterListing()  ← US076 invariant
```

### Invariants (tested in `PilotRosterListingTest`)

| ID | Invariant | Test |
|----|-----------|------|
| INV-1 | Only `PilotStatus.ACTIVE` is eligible for the roster | `activePilotShouldBeEligibleForRosterListing` |
| INV-1 | `INACTIVE`, `SUSPENDED`, `RETIRED` excluded | `inactive/suspended/retired...` |
| INV-2 | `certifiedModels` immutable for presentation | `certifiedModelsShouldBeUnmodifiable` |

### Cross-aggregate references

- `Pilot` → `CompanyId` (company identity; does not modify `AirTransportCompany`)
- `Pilot` → `AircraftModelId` (certifications; read-only lookup in listing via `AircraftModelRepository`)

> **Note:** The global DDD model includes `UserId` on `Pilot`. In the current implementation (US075), the user link is reflected via embedded `Email` and `FullName`. The class diagram documents `UserId` as a conceptual reference.

---

## 5. Application-level rules (tested in `ListPilotRosterControllerTest`)

| # | Check | Where | On failure |
|---|-------|-------|------------|
| 1 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `listPilotRoster()` — `AuthorizationPort` | `AuthzException` |
| 2 | ATCC linked to a `CompanyId` | `PilotRosterListing.requireCompanyOfUser` | `IllegalStateException` |
| 3 | Query `findActiveByCompanyId(companyId)` | `PilotRepository` | — |
| 4 | Mapping `Pilot` → `PilotRosterListItemDTO` via `toRow(pilot, modelRepo)` | `PilotRosterListing` | — |
| 5 | Sort by `fullName` ascending | `ListPilotRosterController` | — |
| 6 | Empty list → `NO_ACTIVE_PILOTS` | `PilotRosterListResult` | — |
| 7 | Operation without aggregate mutation | Controller read-only | — |
| 8 | Certified model labels resolved per `AircraftModelId` | `PilotRosterListing.resolveCertifiedModelLabels` | Falls back to model id string |
