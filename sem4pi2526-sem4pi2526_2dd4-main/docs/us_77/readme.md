# US077 — Remove (deactivate) a pilot

## 1. Context

As an **Air Transport Company Collaborator (ATCC)**, I want to remove a pilot from my company's roster by deactivating them (`ACTIVE` → `INACTIVE`).

**Remove** means a status change, not deletion. A deactivated pilot no longer appears in the US076 roster.

**Critical invariant:** a pilot with **assigned flight plans** cannot be deactivated.

**Dependencies:** US030, US060/061, US075, US080 (flight plan assignments).

**Current status:** Implemented (domain, application, persistence, console UI and tests).

Supporting documentation: [requirements/requisitos.md](requirements/requisitos.md), [design/design.md](design/design.md), [analyse/analise.md](analyse/analise.md), [tests/testes.md](tests/testes.md).

---

## 2. Requirements

**US077** As an Air Transport Company Collaborator, I want to remove a pilot from my company's roster.

**Acceptance criteria (derived from tests):**

| ID | Criterion | Traceability test |
|----|-----------|-------------------|
| AC-01 | Only `ACTIVE` pilots can be deactivated | `DeactivatePilotTest` [INV-2] |
| AC-02 | Deactivation sets status to `INACTIVE` when allowed | `deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans` |
| AC-03 | Pilot with assigned flight plans cannot be deactivated | `deactivateShouldRejectWhenPilotHasAssignedFlightPlans` |
| AC-04 | Already `INACTIVE` pilot cannot be deactivated again | `deactivateShouldRejectWhenPilotAlreadyInactive` |
| AC-05 | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` required | `deactivateShouldSetPilotInactiveAndPersist` |
| AC-06 | Operation restricted to the ATCC's company | `deactivateShouldFailWhenPilotBelongsToAnotherCompany` |
| AC-07 | ATCC not linked to a company → failure | `deactivateShouldFailWhenUserNotLinkedToCompany` |
| AC-08 | Pilot not found → failure | `deactivateShouldFailWhenPilotNotFound` |
| AC-09 | Flight plans queried before deactivation | `deactivateShouldQueryAssignedFlightPlansForPilot` |
| AC-10 | Successful deactivation is persisted | `deactivateShouldSetPilotInactiveAndPersist` |
| AC-11 | Deactivated pilot blocked from new flight plan assignment | `RegisterFlightPlanControllerTest.shouldThrowWhenPilotInactive` |

**Role:** `AIR_TRANSPORT_COMPANY_COLLABORATOR`.

---

## 3. Analysis

### 3.1 Actor

| Actor | System role |
|-------|-------------|
| **Air Transport Company Collaborator** | Authenticated user who manages the company's pilot roster |

### 3.2 Business rules (domain)

Validated in `DeactivatePilotTest`:

- **BR-01:** Deactivation transitions `PilotStatus.ACTIVE` → `INACTIVE`.
- **BR-02:** Assigned flight plans block deactivation (`deactivate(true)` → exception).
- **BR-03:** Only `ACTIVE` pilots are eligible; `INACTIVE`, `SUSPENDED`, `RETIRED` are rejected.

### 3.3 Application rules

Validated in `DeactivatePilotControllerTest`:

- **BR-04:** The ATCC's `CompanyId` is resolved from the authenticated username.
- **BR-05:** The pilot must belong to that `CompanyId`.
- **BR-06:** `FlightPlanRepository.hasAssignedFlightPlansFor(pilotId)` is called before `Pilot.deactivate`.
- **BR-07:** The updated pilot is persisted via `PilotRepository.save`.
- **BR-08:** Inactive pilots are excluded from `RegisterFlightPlanController.listPilots()` and rejected by `FlightPlanFactory.pilotIsActive` (US080).

### 3.4 Validations

| # | Component | Check | Where | On failure |
|---|-----------|-------|-------|------------|
| 1 | `DeactivatePilotController` | Role `AIR_TRANSPORT_COMPANY_COLLABORATOR` | `deactivatePilot()` — `AuthorizationPort` | `AuthzException` |
| 2 | `PilotRosterListing` | ATCC linked to a company | `requireCompanyOfUser()` | `IllegalStateException` |
| 3 | `PilotRepository` | Pilot exists | `ofIdentity(pilotId)` | `IllegalArgumentException` |
| 4 | `DeactivatePilotController` | Pilot belongs to ATCC's company | company comparison | `IllegalArgumentException` |
| 5 | `FlightPlanRepository` | No assigned flight plans | `hasAssignedFlightPlansFor` | — |
| 6 | `Pilot` | Deactivation invariants | `deactivate(hasPlans)` | `IllegalStateException` |
| 7 | `PilotRepository` | Persist change | `save(pilot)` | — |

---

## 4. Design

### 4.1 Layered architecture

| Layer | Component | Responsibility |
|-------|-----------|----------------|
| **Presentation** | `DeactivatePilotUI` | Collects `PilotId` and invokes the controller |
| **Application** | `DeactivatePilotController` | Orchestrates auth, scope, flight-plan check, deactivation, save |
| **Application** | `PilotRosterListing` | Reuses `requireCompanyOfUser` from US076 |
| **Domain** | `Pilot` (AR), `PilotStatus` | Status transition and invariants |
| **Infrastructure** | `PilotRepository`, `FlightPlanRepository` | Load, query assignments, persist |
| **Infrastructure** | `AuthorizationPort` | Authentication and roles (US030) |

### 4.2 Class diagram

Source: [design/cd_us_77.puml](design/cd_us_77.puml)  
Generated: [design/CD_US077_DeactivatePilot.svg](design/CD_US077_DeactivatePilot.svg)

Includes `DeactivatePilotUI`, `ListPilotRosterController` (pilot selection), `DeactivatePilotController`, and `FlightPlanRepository.hasAssignedFlightPlansFor`.

### 4.3 Sequence diagram

Source: [design/sd_us_77.puml](design/sd_us_77.puml)  
Generated: [design/SD_US077_DeactivatePilot.svg](design/SD_US077_DeactivatePilot.svg)

Regenerate SVGs from the repo root (requires Java 11+): `./generate-plantuml-diagrams.sh`

### 4.4 Flow

1. `DeactivatePilotUI` lists active pilots via `ListPilotRosterController`, then invokes `DeactivatePilotController.deactivatePilot(pilotId)`.
2. Controller verifies role `AIR_TRANSPORT_COMPANY_COLLABORATOR`.
3. Resolves `CompanyId` via `PilotRosterListing.requireCompanyOfUser`.
4. Loads pilot via `PilotRepository.ofIdentity(pilotId)`.
5. Verifies `pilot.getCompany()` matches the ATCC's company.
6. Queries `FlightPlanRepository.hasAssignedFlightPlansFor(pilotId)`.
7. Calls `pilot.deactivate(hasPlans)` — domain enforces invariants.
8. Persists via `PilotRepository.save(pilot)`.

### 4.5 Cross-aggregate design

| Aggregate | Role in US077 |
|-----------|---------------|
| `Pilot` | Modified — status `ACTIVE` → `INACTIVE` |
| `FlightPlan` | Read-only — assignment check by `PilotId` |
| `AirTransportCompany` | Not modified — scope via `CompanyId` reference |

The application passes `hasAssignedFlightPlans` as a `boolean` to `Pilot.deactivate()`, keeping the domain independent of `FlightPlan` entity types.

---

## 5. Test strategy (TDD)

The design was **guided by unit tests written before business implementation**.

### 5.1 Covered scenarios

| Scenario | Test | Layer |
|----------|------|-------|
| **Success** | `deactivateShouldSetPilotInactiveAndPersist` | Application |
| **Flight plan guard** | `deactivateShouldRejectWhenPilotHasAssignedFlightPlans` | Domain + Application |
| **Company scope** | `deactivateShouldFailWhenPilotBelongsToAnotherCompany` | Application |
| **Authorization/context** | `deactivateShouldFailWhenUserNotLinkedToCompany` | Application |
| **Status transition** | `deactivateShouldSetStatusToInactiveWhenNoAssignedFlightPlans` | Domain |
| **Non-ACTIVE rejection** | `deactivateShouldRejectWhenPilotIsNotActive` | Domain |

Full detail: [tests/testes.md](tests/testes.md).

### 5.2 Test doubles (NFR03 / NFR08)

| Double | Interface | File |
|--------|-----------|------|
| `FakeAuth` | `AuthorizationPort` | `PilotRosterListingTestDoubles` |
| `TestPilotRepo` | `PilotRepository` | `PilotRosterListingTestDoubles` |
| `TestCompanyRepo` | `AirTransportCompanyRepository` | `PilotRosterListingTestDoubles` |
| `TestFlightPlanRepo` | `FlightPlanRepository` | `PilotRosterListingTestDoubles` |

Manual test doubles (no Mockito), injected via constructor — same pattern as US076.

---

## 6. Implementation

| Layer | Files | Status |
|-------|-------|--------|
| **Domain** | `Pilot.java` (+ `deactivate`) | ✅ Done |
| **Domain** | `FlightPlanRepository.java` (+ `hasAssignedFlightPlansFor`) | ✅ Done |
| **Application** | `DeactivatePilotController.java` | ✅ Done |
| **Persistence** | `InMemoryFlightPlanRepository`, `JpaFlightPlanRepository` | ✅ Done |
| **Presentation** | `DeactivatePilotUI` | ✅ Done |

### 6.1 Package locations

| Conceptual layer | Maven module | Java package |
|------------------|--------------|--------------|
| **core.domain** | `aisafe.core` | `eapli.aisafe.pilot`, `eapli.aisafe.flightPlan.repositories` |
| **application** | `aisafe.infrastructure.application` | `eapli.aisafe.pilot.application` |
| **infrastructure.persistence** | `aisafe.persistence.impl` | `eapli.aisafe.persistence.impl.jpa` / `.inmemory` |

---

## 7. Integration / demonstration

1. Bootstrap: company + ATCC + active pilots (US060/061/075).
2. Optionally register a flight plan for a pilot (US080).
3. Login as ATCC.
4. Menu: "Remove pilot from roster".
5. Deactivate a pilot **without** flight plans → confirm `INACTIVE`.
6. List roster (US076) → pilot no longer appears.
7. Attempt deactivation of a pilot **with** flight plans → expect failure.

```bash
plantuml docs/us_77/design/cd_us_77.puml docs/us_77/design/sd_us_77.puml
mvn test -pl aisafe.core,aisafe.infrastructure.application \
  -Dtest=DeactivatePilotTest,DeactivatePilotControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

---

## 8. Files created

### Documentation
- `docs/us_77/readme.md`
- `docs/us_77/requirements/requisitos.md`
- `docs/us_77/design/design.md`
- `docs/us_77/analyse/analise.md`
- `docs/us_77/tests/testes.md`
- `docs/us_77/design/cd_us_77.puml`
- `docs/us_77/design/sd_us_77.puml`

### Code (US077)
- `aisafe.core/.../pilot/Pilot.java` (`deactivate` method)
- `aisafe.core/.../flightPlan/repositories/FlightPlanRepository.java` (+ `hasAssignedFlightPlansFor`)
- `aisafe.core/.../test/.../DeactivatePilotTest.java`
- `aisafe.infrastructure.application/.../DeactivatePilotController.java`
- `aisafe.infrastructure.application/.../DeactivatePilotControllerTest.java`
- `aisafe.infrastructure.application/.../PilotRosterListingTestDoubles.java` (+ `TestFlightPlanRepo`)
- `aisafe.persistence.impl/.../InMemoryFlightPlanRepository.java`
- `aisafe.persistence.impl/.../JpaFlightPlanRepository.java`

---

## 9. Observations

- Deactivation is a **soft remove** — the `Pilot` aggregate remains in the database with `INACTIVE` status.
- `FlightPlan` is queried by `PilotId` without modifying the `FlightPlan` aggregate (DDD reference by identity).
- `PilotRosterListing.requireCompanyOfUser` is **reused** from US076 for company resolution.
- `JpaFlightPlanRepository.hasAssignedFlightPlansFor` queries flight plans by embedded `pilot.value` (`matchOne("e.pilot.value = :pilotId", ...)`).
- For JPA deployment, register `FlightPlan` in `persistence.xml` so assignments are persisted and queryable.
- Inactive pilots are excluded from new flight plan assignment via `FlightPlanFactory.pilotIsActive` (US080) and from `RegisterFlightPlanController.listPilots()`.
- `DeactivatePilotUI` reuses `ListPilotRosterController` to show only active pilots before deactivation.
- After deactivation, US076 roster listing excludes the pilot via `findActiveByCompanyId` / `isEligibleForRosterListing()`.
